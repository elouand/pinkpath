
require('dotenv').config();
const express = require('express');
const { PrismaClient } = require('@prisma/client');
const { Pool } = require('pg');
const { PrismaPg } = require('@prisma/adapter-pg');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const axios = require('axios');
const PDFDocument = require('pdfkit');
const cache = new Map();

const JWT_SECRET = "ton_secret_ultra_confidentiel"; // Change ça pour la prod
// 1. Configuration de la connexion PostgreSQL
const connectionString = process.env.DATABASE_URL;
const pool = new Pool({ connectionString });
const adapter = new PrismaPg(pool);
const prisma = new PrismaClient({ adapter });

const app = express();

// --- CACHE EN MÉMOIRE POUR LES DÉTAILS DE LIEUX ---
const placeDetailsCache = new Map();
const CACHE_DURATION = 1000 * 60 * 60; // 1 heure

// --- CONFIGURATION ---
// IMPORTANT : Remplace "0.0.0.0" ici par ton IP réelle (ex: 192.168.1.XX)
// pour que ton téléphone puisse charger les images !
const SERVER_IP = "192.168.1.187"//"192.168.1.25"; 
const PORT = 3000;
const BASE_URL = `http://${SERVER_IP}:${PORT}`;

app.use(cors());
app.use(express.json());

// Création du dossier uploads
const uploadDir = 'uploads';
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir);
}

// Configuration de Multer
const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, 'uploads/'),
    filename: (req, file, cb) => cb(null, Date.now() + path.extname(file.originalname))
});
const upload = multer({ storage: storage });

// Logs pour les fichiers demandés
app.use('/uploads', (req, res, next) => {
    console.log(`🖼️  Fichier demandé : ${req.url}`);
    next();
});
app.use('/uploads', express.static('uploads'));

// --- ROUTES ---

/** [POST] Publier une photo + audio */
app.post('/api/photos', upload.fields([
    { name: 'image', maxCount: 1 }, 
    { name: 'audio', maxCount: 1 }
]), async (req, res) => {
    try {
        const { description, type_lieu, latitude, longitude, is_public, authorId, groupId, tags } = req.body;

        if (!req.files || !req.files['image']) {
            return res.status(400).json({ error: "Image manquante" });
        }

        // Nettoyage des tags : transforme "Rando, Nature" en ["rando", "nature"]
        const tagsArray = tags ? tags.split(',').map(t => t.trim().toLowerCase()).filter(t => t !== "") : [];

        const photo = await prisma.photo.create({
            data: {
                url: `/uploads/${req.files['image'][0].filename}`,
                audioUrl: req.files['audio'] ? `/uploads/${req.files['audio'][0].filename}` : null,
                description: description || "",
                type_lieu: type_lieu || "Inconnu",
                latitude: parseFloat(latitude) || 0,
                longitude: parseFloat(longitude) || 0,
                is_public: is_public === 'true',
                authorId: authorId ? parseInt(authorId) : null,
                groupId: groupId ? parseInt(groupId) : null,
                likesCount: 0,
                // --- LOGIQUE DES TAGS ---
                tags: {
                    connectOrCreate: tagsArray.map(tagName => ({
                        where: { name: tagName },
                        create: { name: tagName }
                    }))
                }
            },
            include: { tags: true }
        });

        res.status(201).json(photo);
    } catch (error) {
        console.error("💥 Erreur upload :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});


app.get('/api/photos', async (req, res) => {
    const userId = req.query.userId ? parseInt(req.query.userId) : null;

    try {
        const photos = await prisma.photo.findMany({
            where: {
                OR: [
                    { is_public: true }, // 1. Photos marquées publiques
                    { group: { isPublic: true } }, // 2. Photos dans des groupes publics
                    ...(userId ? [
                        { authorId: userId }, // 3. Mes propres photos
                        { 
                            group: { 
                                members: { some: { userId: userId } } // 4. Photos de mes groupes
                            } 
                        }
                    ] : [])
                ]
            },
            include: {
                author: true,
                tags: true,
                group: true,
                _count: { select: { comments: true } },
                likedBy: userId ? { where: { userId: userId } } : false,
                itinerary: { include: { steps: { orderBy: { order: 'asc' } } } }
            },
            orderBy: { date: 'desc' }
        });

        const formattedPosts = photos.map(p => ({
            id: p.id.toString(),
            title: p.type_lieu,
            content: p.description,
            latitude: p.latitude,
            longitude: p.longitude,
            imageUrl: p.url ? `${BASE_URL}${p.url}` : null,
            audioUrl: p.audioUrl ? `${BASE_URL}${p.audioUrl}` : null,
            author: p.author ? (p.author.pseudo || p.author.username) : "Anonyme",
            authorAvatarUrl: p.author && p.author.profileUrl ? `${BASE_URL}${p.author.profileUrl}` : null,
            likes: p.likesCount || 0,
            commentCount: p._count ? p._count.comments : 0,
            isLiked: p.likedBy && p.likedBy.length > 0,
            tags: p.tags.map(t => t.name),
            groupName: p.group ? p.group.name : null,
            isPublic: p.is_public,
            itineraryId: p.itineraryId || null,
            sharedItinerary: p.itinerary ? {
                id: p.itinerary.id,
                name: p.itinerary.name,
                duration: p.itinerary.duration,
                distance: p.itinerary.distance,
                mode: p.itinerary.mode,
                steps: p.itinerary.steps.map(s => ({
                    id: s.id, order: s.order, name: s.name,
                    type: s.type, latitude: s.latitude, longitude: s.longitude
                }))
            } : null
        }));

        res.json(formattedPosts);
    } catch (error) {
        console.error("💥 Erreur flux :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});



/** [POST] Créer un nouveau groupe et y ajouter le créateur */
app.post('/api/groups', upload.single('groupImage'), async (req, res) => {
    try {
        const { name, description, isPublic, userId } = req.body;
        const file = req.file;

        const group = await prisma.group.create({
            data: {
                name,
                description,
                isPublic: isPublic === 'true',
                imageUrl: file ? `/uploads/${file.filename}` : null,
                members: {
                    create: {
                        userId: parseInt(userId),
                        role: 'ADMIN' // Le créateur est Admin par défaut
                    }
                }
            }
        });
        res.status(201).json(group);
    } catch (error) {
        res.status(500).json({ error: "Erreur création groupe" });
    }
});

/** [GET] Récupérer les groupes auxquels appartient un utilisateur */
app.get('/api/users/:userId/groups', async (req, res) => {
    try {
        const { userId } = req.params;
        
        const userMemberships = await prisma.groupMember.findMany({
            where: { userId: parseInt(userId) },
            include: {
                group: {
                    include: {
                        // On récupère les 3 premiers membres pour les avatars de la carte
                        members: {
                            take: 3,
                            include: {
                                user: {
                                    select: { id: true, profileUrl: true,username: true, pseudo: true }
                                }
                            }
                        },
                        // On récupère les compteurs globaux
                        _count: { 
                            select: { members: true, photos: true } 
                        }
                    }
                }
            }
        });

        const formattedGroups = userMemberships.map(membership => ({
            id: membership.group.id,
            name: membership.group.name,
            description: membership.group.description,
            imageUrl: membership.group.imageUrl ? `${BASE_URL}${membership.group.imageUrl}` : null,
            isPublic: membership.group.isPublic,
            userRole: membership.role,
            
            // On transforme les membres pour correspondre à group.users sur Android
            users: membership.group.members.map(m => ({
                id: m.user.id,
                profileUrl: m.user.profileUrl ? `${BASE_URL}${m.user.profileUrl}` : null
            })),

            // On crée l'objet "count" attendu par Kotlin
            count: {
                users: membership.group._count.members,
                photos: membership.group._count.photos,
                paths: 0 // À remplacer par ton compteur d'itinéraires quand tu auras le modèle
            }
        }));

        res.json(formattedGroups);
    } catch (error) {
        console.error("💥 Erreur récupération groupes :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});


/** [GET] Récupérer les détails d'un groupe (Membres avec rôles + Photos/Itinéraires) */
/** [GET] Récupérer les détails d'un groupe (Membres avec rôles + Flux du groupe) */
/** [GET] Récupérer les détails d'un groupe (Membres + Flux + Previews) */
// =============================================================
// 1. RECHERCHE GLOBALE (DOIT ÊTRE AVANT :groupId)
// =============================================================
app.get('/api/groups/search', async (req, res) => {    const { query, userId } = req.query; // Le userId est maintenant envoyé par l'app
    if (!query) return res.json([]);

    try {
        const groups = await prisma.group.findMany({
            where: { name: { contains: query, mode: 'insensitive' } },
            include: {
                _count: { select: { members: true, photos: true } },
                // VÉRIFICATION DES DEMANDES EN ATTENTE
                joinRequests: userId ? { where: { userId: parseInt(userId) } } : false,
                members: userId ? { where: { userId: parseInt(userId) } } : false
            }
        });

        res.json(groups.map(g => ({
            id: g.id,
            name: g.name,
            description: g.description,
            imageUrl: g.imageUrl ? `${BASE_URL}${g.imageUrl}` : null,
            isPublic: g.isPublic,
            // CES DEUX LIGNES SONT CRITIQUES POUR L'APP
            isPending: g.joinRequests?.length > 0, 
            userRole: g.members?.[0]?.role || null,
            count: { users: g._count.members, photos: g._count.photos, paths: 0 }
        })));
    } catch (e) { res.status(500).json({ error: "Erreur" }); }
});

// =============================================================
// 2. DÉTAILS D'UN GROUPE
// =============================================================
app.get('/api/groups/:groupId', async (req, res) => {
    try {
        const { groupId } = req.params;
        const userId = req.query.userId ? parseInt(req.query.userId) : null; // <-- Ajout pour les Likes
        const idInt = parseInt(groupId);
        if (isNaN(idInt)) return res.status(400).json({ error: "ID invalide" });

        const group = await prisma.group.findUnique({
            where: { id: idInt },
            include: {
                members: { include: { user: { select: { id: true, username: true, pseudo: true, profileUrl: true } } } },
                photos: {
                    include: {
                        author: true,
                        tags: true, // <-- Inclus pour les badges de l'app
                        likedBy: userId ? { where: { userId: userId } } : false, // <-- Pour le coeur rouge
                        itinerary: { include: { steps: { orderBy: { order: 'asc' } } } },
                        _count: { select: { comments: true } }
                    },
                    orderBy: { date: 'desc' }
                }
            }
        });

        if (!group) return res.status(404).json({ error: "Groupe non trouvé" });

        res.json({
            id: group.id,
            name: group.name,
            description: group.description,
            imageUrl: group.imageUrl ? `${BASE_URL}${group.imageUrl}` : null,
            isPublic: group.isPublic,
            users: group.members.map(m => ({
                id: m.user.id.toString(),
                username: m.user.username,
                pseudo: m.user.pseudo,
                profileUrl: m.user.profileUrl ? `${BASE_URL}${m.user.profileUrl}` : null,
                role: m.role
            })),
            photos: group.photos.map(p => ({
                id: p.id.toString(),
                title: p.type_lieu,
                content: p.description,
                imageUrl: p.url ? `${BASE_URL}${p.url}` : null,
                audioUrl: p.audioUrl ? `${BASE_URL}${p.audioUrl}` : null, // <-- Ajouté !
                date: p.date, // <-- Ajouté !
                author: p.author?.pseudo || p.author?.username || "Anonyme",
                authorAvatarUrl: p.author?.profileUrl ? `${BASE_URL}${p.author.profileUrl}` : null,
                likes: p.likesCount || 0,
                isLiked: p.likedBy && p.likedBy.length > 0, // <-- Ajouté !
                commentCount: p._count?.comments || 0,
                tags: p.tags.map(t => t.name), // <-- Ajouté !
                sharedItinerary: p.itinerary ? {
                    id: p.itinerary.id,
                    name: p.itinerary.name,
                    duration: p.itinerary.duration,
                    distance: p.itinerary.distance,
                    mode: p.itinerary.mode,
                    steps: p.itinerary.steps
                } : null
            }))
        });
    } catch (error) {
        console.error("💥 Erreur détails :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});

// =============================================================
// 3. ACTIONS (REJOINDRRE / DEMANDES / AJOUT)
// =============================================================

/** Rejoindre ou demander à rejoindre */
app.post('/api/groups/:groupId/join', async (req, res) => {
    const { groupId } = req.params;
    const { userId } = req.body;
    try {
        const group = await prisma.group.findUnique({ where: { id: parseInt(groupId) } });
        const existing = await prisma.groupMember.findUnique({
            where: { userId_groupId: { userId: parseInt(userId), groupId: parseInt(groupId) } }
        });
        if (existing) return res.status(400).json({ error: "Déjà membre" });

        if (group.isPublic) {
            await prisma.groupMember.create({
                data: { userId: parseInt(userId), groupId: parseInt(groupId), role: 'MEMBER' }
            });
            res.json({ message: "Groupe rejoint !" });
        } else {
            await prisma.joinRequest.create({ data: { userId: parseInt(userId), groupId: parseInt(groupId) } });
            res.json({ message: "Demande envoyée !" });
        }
    } catch (e) { res.status(400).json({ error: "Action impossible" }); }
});

/** [GET] Voir les demandes pour un groupe */
app.get('/api/groups/:groupId/requests', async (req, res) => {
    try {
        const requests = await prisma.joinRequest.findMany({
            where: { groupId: parseInt(req.params.groupId) },
            include: { user: true }
        });
        res.json(requests.map(r => ({
            id: r.id,
            userId: r.userId,
            username: r.user.pseudo || r.user.username,
            userAvatar: r.user.profileUrl ? `${BASE_URL}${r.user.profileUrl}` : null,
            createdAt: r.createdAt
        })));
    } catch (e) { res.status(500).json({ error: "Erreur" }); }
});

/** [POST] Répondre à une demande */
app.post('/api/groups/:groupId/requests/:requestId/respond', async (req, res) => {
    const { groupId, requestId } = req.params;
    const { action } = req.body; 

    try {
        const request = await prisma.joinRequest.findUnique({ where: { id: parseInt(requestId) } });
        if (!request) return res.status(404).json({ error: "Demande introuvable" });

        if (action === 'accept') {
            await prisma.groupMember.create({
                data: { userId: request.userId, groupId: parseInt(groupId), role: 'MEMBER' }
            });
        }
        await prisma.joinRequest.delete({ where: { id: parseInt(requestId) } });
        res.json({ message: "Ok" });
    } catch (e) { res.status(500).json({ error: "Erreur" }); }
});
/** [POST] Ajouter un utilisateur à un groupe via son pseudo */
app.post('/api/groups/:groupId/add-user', async (req, res) => {
    try {
        const { groupId } = req.params;
        const { usernameToAdd } = req.body;

        await prisma.groupMember.create({
            data: {
                group: { connect: { id: parseInt(groupId) } },
                user: { connect: { username: usernameToAdd } },
                role: 'MEMBER'
            }
        });

        res.json({ message: `${usernameToAdd} a été ajouté au groupe !` });
    } catch (error) {
        console.error("💥 Erreur ajout membre :", error);
        res.status(404).json({ error: "Utilisateur introuvable ou déjà dans le groupe" });
    }
});





app.post('/api/auth/register', async (req, res) => {
    console.log("👤 Tentative de création de compte...");
    try {
        const { username, password, pseudo, email } = req.body; // Récupération des nouveaux champs

        const hashedPassword = await bcrypt.hash(password, 10);

        const user = await prisma.user.create({
            data: { 
                username, 
                password: hashedPassword,
                pseudo: pseudo || username, // Si pas de pseudo fourni, on met l'username par défaut
                email: email
            }
        });

        console.log(`✅ Utilisateur créé : ${username} (Pseudo: ${user.pseudo})`);
        res.status(201).json({ userId: user.id, pseudo: user.pseudo });

    } catch (error) {
        console.error("❌ ERREUR DÉTAILLÉE :", error); 
        res.status(500).json({ error: "Erreur serveur", code: error.code });
    }
});



/** [PATCH] Mettre à jour la photo de profil */
app.patch('/api/auth/profile-picture', upload.single('avatar'), async (req, res) => {
    console.log("🖼️ Mise à jour de la photo de profil...");
    try {
        const { userId } = req.body; // L'ID de l'utilisateur envoyé par l'app

        if (!req.file) {
            return res.status(400).json({ error: "Aucune image reçue" });
        }

        const updatedUser = await prisma.user.update({
            where: { id: parseInt(userId) },
            data: {
                profileUrl: `/uploads/${req.file.filename}`
            }
        });

        console.log(`✅ Photo de profil mise à jour pour : ${updatedUser.username}`);
        res.json({ 
            message: "Photo mise à jour", 
            profileUrl: `${BASE_URL}${updatedUser.profileUrl}` 
        });

    } catch (error) {
        console.error(error);
        res.status(500).json({ error: "Erreur lors de l'upload de l'avatar" });
    }
});



app.post('/api/auth/login', async (req, res) => {
    console.log("🔑 Tentative de connexion...");
    try {
        const { username, password } = req.body;

        // 1. Chercher l'utilisateur
        const user = await prisma.user.findUnique({ where: { username } });
        if (!user) {
            console.log("❌ Utilisateur non trouvé");
            return res.status(401).json({ error: "Utilisateur non trouvé" });
        }

        // 2. Vérifier le mot de passe
        const validPassword = await bcrypt.compare(password, user.password);
        if (!validPassword) {
            console.log("❌ Mot de passe incorrect");
            return res.status(401).json({ error: "Mot de passe incorrect" });
        }

        // 3. GÉNÉRER LE TOKEN
        const token = jwt.sign(
            { userId: user.id, username: user.username }, 
            JWT_SECRET, 
            { expiresIn: '24h' }
        );

        console.log(`🔓 Connexion réussie pour : ${username}`);

        // 4. Envoyer la réponse
        res.json({ 
        message: "Connexion réussie",
        token: token,
        user: { 
            id: user.id, 
            username: user.username,
            pseudo: user.pseudo, // On renvoie le pseudo
            email: user.email,   // On renvoie l'email
            profileUrl: user.profileUrl ? `${BASE_URL}${user.profileUrl}` : null
        }
    });

    } catch (error) {
        console.error("💥 Erreur Login :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});




/** [POST] Ajouter un commentaire à une photo */
app.post('/api/photos/:photoId/comments', async (req, res) => {
    console.log(`💬 Nouveau commentaire sur la photo ${req.params.photoId}`);
    try {
        const { photoId } = req.params;
        const { text, userId } = req.body; // L'ID de l'utilisateur qui commente

        if (!text || !userId) {
            return res.status(400).json({ error: "Texte ou ID utilisateur manquant" });
        }

        const comment = await prisma.comment.create({
            data: {
                text: text,
                photoId: parseInt(photoId),
                authorId: parseInt(userId)
            },
            include: { author: true } // On inclut l'auteur pour renvoyer le pseudo direct
        });

        // On renvoie le commentaire formaté exactement comme le GET
        // pour que ton app puisse l'ajouter à la liste instantanément
        res.status(201).json({
            id: comment.id,
            text: comment.text,
            date: comment.createdAt,
            authorName: comment.author.pseudo || comment.author.username,
            authorAvatarUrl: comment.author.profileUrl ? `${BASE_URL}${comment.author.profileUrl}` : null
        });

    } catch (error) {
        console.error("💥 Erreur creation commentaire :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});

/** [GET] Récupérer les commentaires d'une photo */
app.get('/api/photos/:photoId/comments', async (req, res) => {
    try {
        const { photoId } = req.params;
        const comments = await prisma.comment.findMany({
            where: { photoId: parseInt(photoId) },
            include: { author: true },
            orderBy: { createdAt: 'asc' }
        });

        const formattedComments = comments.map(c => ({
            id: c.id,
            text: c.text,
            date: c.createdAt,
            authorName: c.author.pseudo || c.author.username,
            authorAvatarUrl: c.author.profileUrl ? `${BASE_URL}${c.author.profileUrl}` : null
        }));

        res.json(formattedComments);
    } catch (error) {
        console.error("💥 Erreur récupération commentaires :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});

app.post('/api/photos/:photoId/like', async (req, res) => {
    const { photoId } = req.params;
    const { userId } = req.body; // L'ID de l'utilisateur qui clique

    if (!userId) return res.status(400).json({ error: "ID utilisateur manquant" });

    try {
        // 1. Vérifier si le like existe déjà
        const existingLike = await prisma.like.findUnique({
            where: {
                userId_photoId: {
                    userId: parseInt(userId),
                    photoId: parseInt(photoId)
                }
            }
        });

        if (existingLike) {
            // OPTION : Si il existe, on le supprime (Like/Unlike)
            await prisma.like.delete({ where: { id: existingLike.id } });
            
            const photo = await prisma.photo.update({
                where: { id: parseInt(photoId) },
                data: { likesCount: { decrement: 1 } }
            });
            
            return res.json({ message: "Like retiré", likes: photo.likesCount, isLiked: false });
        }

        // 2. Sinon, on crée le like et on incrémente le compteur
        const [newLike, updatedPhoto] = await prisma.$transaction([
            prisma.like.create({
                data: { userId: parseInt(userId), photoId: parseInt(photoId) }
            }),
            prisma.photo.update({
                where: { id: parseInt(photoId) },
                data: { likesCount: { increment: 1 } }
            })
        ]);

        res.json({ message: "Photo likée", likes: updatedPhoto.likesCount, isLiked: true });

    } catch (error) {
        console.error("💥 Erreur Like :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});

/**
 * [GET] /api/places/nearby
 * Récupère les lieux autour de l'utilisateur en temps réel depuis OpenStreetMap
 */
async function fetchWithRetry(url, data, retries = 2) {

    try {
        const params = new URLSearchParams({ data });
        return await axios.post(url, params.toString(), {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'User-Agent': 'TravelingApp/1.0 (contact@traveling.app)'
            },
            timeout: 15000
        });
    } catch (error) {
        if (retries > 0) {
            await new Promise(r => setTimeout(r, 1000));
            return fetchWithRetry(url, data, retries - 1);
        }
        throw error;
    }
}

app.get('/api/places/nearby', async (req, res) => {

    try {
        console.log(`nearby request received`);
        const { lat, lon } = req.query;

        if (!lat || !lon) {
            return res.status(400).json({ error: "Latitude et Longitude manquantes" });
        }

        const latitude = parseFloat(lat);
        const longitude = parseFloat(lon);

        if (isNaN(latitude) || isNaN(longitude)) {
            return res.status(400).json({ error: "Coordonnées invalides" });
        }

        // 🔑 Cache (clé arrondie pour éviter trop d'entrées)
        const cacheKey = `${latitude.toFixed(3)},${longitude.toFixed(3)}`;
        if (cache.has(cacheKey)) {
            console.log("⚡ Réponse depuis le cache");
            return res.json(cache.get(cacheKey));
        }

        const radius = 1000;
        const overpassUrl = "https://overpass-api.de/api/interpreter";

        const overpassQuery = `
            [out:json][timeout:10];
            (
                node["amenity"~"restaurant|cafe|bar|pub|fast_food|food_court"](around:${radius},${latitude},${longitude});
                node["tourism"~"hotel|museum|attraction|gallery|viewpoint|zoo"](around:${radius},${latitude},${longitude});
                node["leisure"~"park|garden|playground|stadium"](around:${radius},${latitude},${longitude});
            );
            out body;
        `;

        let response;

        try {
            response = await fetchWithRetry(overpassUrl, overpassQuery);
        } catch (error) {
            if (error.response && (error.response.status === 429 || error.response.status === 504)) {
                console.log("⚠️ Overpass surchargé → fallback cache ou vide");

                if (cache.has(cacheKey)) {
                    return res.json(cache.get(cacheKey));
                }

                return res.json([]);
            }
            throw error;
        }

        const places = (response.data.elements || [])
            .filter(el => el.tags && el.tags.name && el.lat && el.lon)
            .map(el => ({
                id: el.id.toString(),
                name: el.tags.name,
                type: el.tags.amenity || el.tags.tourism || el.tags.leisure || "Lieu d'intérêt",
                latitude: el.lat,
                longitude: el.lon,
            }));

        // 💾 Mise en cache (TTL simple avec setTimeout)
        cache.set(cacheKey, places);
        setTimeout(() => cache.delete(cacheKey), 5 * 60 * 1000); // 5 minutes

        console.log(`📍 ${places.length} lieux trouvés autour de ${lat},${lon}`);
        res.json(places);

    } catch (error) {
        if (error.response) {
            console.error("💥 Erreur OSM :", error.response.status, error.response.data);
        } else {
            console.error("💥 Erreur OSM :", error.message);
        }

        res.status(500).json({ error: "Erreur lors de la récupération des lieux" });
    }
});

/**
 * [GET] /api/places/:id
 * Récupère les détails complets d'un lieu (photos, avis, horaires, etc.)
 */
app.get('/api/places/:id', async (req, res) => {
    console.log(`🚨 FONCTION DETAILS LIEU APPELEE - ID: ${req.params.id}`);
    try {
        const { id } = req.params;
        console.log(`🔍 Requête détails lieu reçue pour ID: ${id}`);

        if (!id) {
            return res.status(400).json({ error: "ID du lieu manquant" });
        }

        // Validation de l'ID
        const numericId = parseInt(id);
        if (isNaN(numericId) || numericId <= 0) {
            console.log(`❌ ID invalide: ${id}`);
            return res.status(400).json({ error: "ID du lieu invalide" });
        }

        // Vérifier le cache
        if (placeDetailsCache.has(numericId)) {
            const cached = placeDetailsCache.get(numericId);
            if (Date.now() - cached.timestamp < CACHE_DURATION) {
                console.log(`💾 Cache hit pour le lieu ${numericId}`);
                return res.json(cached.data);
            } else {
                placeDetailsCache.delete(numericId);
            }
        }

        // Utiliser API OSM directe (séquentiel pour éviter surcharge)
        console.log(`📡 Requête API OSM directe pour ${numericId}`);
        
        const osmUrls = [
            `https://api.openstreetmap.org/api/0.6/node/${numericId}.json`,
            `https://api.openstreetmap.org/api/0.6/way/${numericId}.json`,
            `https://api.openstreetmap.org/api/0.6/relation/${numericId}.json`
        ];

        let element = null;
        
        // Essayer séquentiellement (node -> way -> relation) pour éviter surcharge
        for (const url of osmUrls) {
            try {
                console.log(`🔍 Tentative ${url}`);
                const res = await axios.get(url, { timeout: 10000 }); // Timeout augmenté à 10s
                const type = url.includes('/node/') ? 'node' : url.includes('/way/') ? 'way' : 'relation';
                console.log(`🔍 Réponse brute ${type} pour ${numericId}:`, JSON.stringify(res.data, null, 2));
                const data = res.data.elements && res.data.elements[0] ? res.data.elements[0] : null;
                if (data) {
                    console.log(`🔍 Data extraite ${type}: présente`);
                    element = { ...data, osm_type: type };
                    break; // On s'arrête au premier succès
                } else {
                    console.log(`🔍 Data extraite ${type}: absente`);
                }
            } catch (err) {
                console.log(`❌ Erreur ${url}: ${err.message}`);
            }
        }

        if (!element) {
            console.log(`❌ Élément ${numericId} introuvable`);
            return res.status(404).json({ error: "Lieu non trouvé" });
        }

        // Pour les ways, récupérer les coordonnées du premier node comme approximation
        if (element.osm_type === 'way' && element.nodes && element.nodes.length > 0) {
            console.log(`🏗️ Way détectée, récupération coordonnées du premier node ${element.nodes[0]}`);
            try {
                const nodeResponse = await axios.get(`https://api.openstreetmap.org/api/0.6/node/${element.nodes[0]}.json`, { timeout: 5000 });
                const nodeData = nodeResponse.data.elements && nodeResponse.data.elements[0];
                if (nodeData) {
                    element.lat = nodeData.lat;
                    element.lon = nodeData.lon;
                    console.log(`📍 Coordonnées way approximées: ${element.lat}, ${element.lon}`);
                }
            } catch (nodeError) {
                console.log(`❌ Impossible de récupérer les coordonnées de la way: ${nodeError.message}`);
            }
        }

        const tags = element.tags || {};
        
        // Récupérer les coordonnées selon le type
        let latitude = element.lat;
        let longitude = element.lon;

        if (!latitude || !longitude) {
            console.log(`❌ Coordonnées manquantes`);
            return res.status(404).json({ error: "Coordonnées du lieu non disponibles" });
        }

        console.log(`📍 Coordonnées: ${latitude}, ${longitude}`);

        // Construction de l'objet lieu détaillé
        const placeDetails = {
            id: element.id.toString(),
            name: tags.name || "Nom inconnu",
            type: tags.amenity || tags.tourism || tags.leisure || "Lieu d'intérêt",
            latitude: latitude,
            longitude: longitude,
            description: tags.description || null,
            website: tags.website || null,
            phone: tags.phone || tags.contact_phone || null,
            email: tags.email || tags.contact_email || null,
            address: {
                street: tags['addr:street'] || null,
                housenumber: tags['addr:housenumber'] || null,
                city: tags['addr:city'] || null,
                postcode: tags['addr:postcode'] || null,
                country: tags['addr:country'] || null
            },
            opening_hours: tags.opening_hours || null,
            cuisine: tags.cuisine || null,
            wheelchair: tags.wheelchair || null,
            fee: tags.fee || null,
            operator: tags.operator || null,
            osm_tags: tags, // Tous les tags OSM bruts
            photos: [],
            reviews: []
        };

        console.log(`📝 Objet placeDetails construit: ${placeDetails.name} (${placeDetails.type})`);

        // Tentative de récupération de photos depuis Wikimedia Commons
        if (tags.name && (tags.tourism === 'attraction' || tags.tourism === 'museum' || tags.tourism === 'gallery')) {
            console.log(`🖼️ Recherche de photos Wikimedia pour: ${tags.name}`);
            try {
                const commonsUrl = `https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url&generator=search&gsrsearch="${encodeURIComponent(tags.name)}"&gsrnamespace=6&iiurlwidth=300`;
                const commonsResponse = await axios.get(commonsUrl);

                if (commonsResponse.data.query && commonsResponse.data.query.pages) {
                    const photos = Object.values(commonsResponse.data.query.pages)
                        .slice(0, 5) // Limiter à 5 photos
                        .filter(page => page.imageinfo && page.imageinfo[0]) // Vérifier que imageinfo existe
                        .map(page => ({
                            url: page.imageinfo[0].url,
                            thumb: page.imageinfo[0].thumburl || page.imageinfo[0].url,
                            title: page.title
                        }));
                    placeDetails.photos = photos;
                    console.log(`📸 ${photos.length} photos trouvées sur Wikimedia`);
                } else {
                    console.log(`📸 Aucune photo trouvée sur Wikimedia`);
                }
            } catch (photoError) {
                console.log(`ℹ️ Erreur récupération photos Wikimedia: ${photoError.message}`);
            }
        } else {
            console.log(`ℹ️ Pas de recherche photos (lieu non touristique)`);
        }

        console.log(`📍 Détails complets récupérés pour le lieu ${id}: ${placeDetails.name}`);
        console.log(`📤 Envoi réponse avec ${placeDetails.photos ? placeDetails.photos.length : 0} photos`);
        
        // Stocker en cache
        placeDetailsCache.set(numericId, {
            data: placeDetails,
            timestamp: Date.now()
        });
        console.log(`💾 Lieu ${numericId} mis en cache (expire dans 1h)`);
        
        res.json(placeDetails);

    } catch (error) {
        if (error.response) {
            const status = error.response.status;
            console.error(`💥 Erreur OSM - HTTP ${status}`);
            if (status === 504 || status === 429) {
                return res.status(503).json({ error: "API OSM surchargée, veuillez réessayer" });
            } else if (status === 400) {
                return res.status(400).json({ error: "Paramètres invalides" });
            }
        } else if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
            console.error(`💥 Timeout OSM API`);
            return res.status(503).json({ error: "Timeout - veuillez réessayer" });
        } else {
            console.error(`💥 Erreur détails lieu: ${error.message}`);
        }
        res.status(500).json({ error: "Erreur lors de la récupération des détails du lieu" });
    }
});

/**
 * [GET] /api/route
 * Retourne un trajet de A vers B avec points, distance et duration.
 * Requête attendue : /api/route?startLat=...&startLon=...&endLat=...&endLon=...
 */
app.get('/api/route', async (req, res) => {
    try {
        console.log("REQ QUERY:", req.query);
        const { startLat, startLon, endLat, endLon } = req.query;
        const requestedMode = req.query.mode || 'driving';
        console.log("MODE RECU :", requestedMode);
        const profileMap = {
            driving: 'driving',
            walking: 'walking',
            bicycle: 'cycling'
        };
        const profile = profileMap[requestedMode];
        if (!profile) {
            return res.status(400).json({ error: "Mode invalide. Utilisez driving, walking ou bicycle." });
        }
        if (!startLat || !startLon || !endLat || !endLon) {
            return res.status(400).json({ error: "startLat, startLon, endLat et endLon sont requis" });
        }

        const originLat = parseFloat(startLat);
        const originLon = parseFloat(startLon);
        const destinationLat = parseFloat(endLat);
        const destinationLon = parseFloat(endLon);

        if ([originLat, originLon, destinationLat, destinationLon].some(v => Number.isNaN(v))) {
            return res.status(400).json({ error: "Coordonnées invalides" });
        }

        const osrmUrl = `https://router.project-osrm.org/route/v1/${profile}/${originLon},${originLat};${destinationLon},${destinationLat}?overview=full&geometries=geojson&steps=false`;
        const response = await axios.get(osrmUrl, {
            headers: { 'User-Agent': 'TravelingApp/1.0' }
        });

        if (!response.data || !response.data.routes || response.data.routes.length === 0) {
            return res.status(404).json({ error: "Aucune route trouvée" });
        }

        const route = response.data.routes[0];
        const coords = route.geometry.coordinates || [];
        const points = coords.map(([lon, lat]) => ({ latitude: lat, longitude: lon }));

        res.json({
            points,
            distance: Math.round(route.distance),
            duration: Math.round(route.duration)
        });
    } catch (error) {
        console.error("💥 Erreur route :", error.message || error);
        if (error.response && error.response.data) {
            console.error(error.response.data);
        }
        res.status(500).json({ error: "Impossible de calculer le trajet" });
    }
});

app.patch('/api/groups/:groupId/role', async (req, res) => {
    const { groupId } = req.params;
    const { targetUserId, newRole, requesterId } = req.body;

    try {
        const requester = await prisma.groupMember.findUnique({
            where: { userId_groupId: { userId: parseInt(requesterId), groupId: parseInt(groupId) } }
        });

        if (!requester || requester.role !== 'ADMIN') {
            return res.status(403).json({ error: "Droits insuffisants" });
        }

        await prisma.groupMember.update({
            where: { userId_groupId: { userId: parseInt(targetUserId), groupId: parseInt(groupId) } },
            data: { role: newRole }
        });
        res.json({ message: "Rôle mis à jour" });
    } catch (error) {
        res.status(500).json({ error: "Échec de mise à jour" });
    }
});






// ─────────────────────────────────────────────────────────
// ITINÉRAIRES
// ─────────────────────────────────────────────────────────

function haversine(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) ** 2 +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(a));
}

// Effort modéré = marche 5 km/h, Gros effort = jogging 8 km/h
const MODE_SPEED = { walking: 83, jogging: 133, driving: 500 }; // m/min
const VISIT_TIME = 20; // min par POI

// Facteur de Tobler normalisé : 1.0 sur terrain plat, < 1 en montée/descente forte
function toblerFactor(elevDiff, distM) {
    if (distM < 1) return 1;
    const slope = elevDiff / distM;
    return Math.max(Math.exp(-3.5 * Math.abs(slope + 0.05)) / 0.8408, 0.3);
}

// Récupère les altitudes via OpenTopoData (fallback = 0 si indisponible)
async function getElevations(coords) {
    if (coords.length === 0) return [];
    try {
        const locations = coords.map(([lat, lon]) => `${lat},${lon}`).join('|');
        const resp = await axios.get(
            `https://api.opentopodata.org/v1/srtm30m?locations=${locations}`,
            { timeout: 4000, headers: { 'User-Agent': 'TravelingApp/1.0' } }
        );
        return (resp.data.results || []).map(r => r.elevation || 0);
    } catch {
        return coords.map(() => 0); // terrain plat par défaut
    }
}

function buildRoute(poisPool, startLat, startLon, startElev, durationMin, speed) {
    let remaining = durationMin;
    let curLat = startLat, curLon = startLon, curElev = startElev;
    const steps = [];
    const pool = [...poisPool];
    let totalDist = 0;

    while (pool.length > 0 && remaining > VISIT_TIME) {
        let bestIdx = -1, bestDist = Infinity;
        for (let i = 0; i < pool.length; i++) {
            const d = haversine(curLat, curLon, pool[i].lat, pool[i].lon);
            if (d < bestDist) { bestDist = d; bestIdx = i; }
        }
        const poi = pool[bestIdx];
        const adjSpeed = speed * toblerFactor((poi.elev || 0) - curElev, bestDist);
        const travelMin = bestDist / adjSpeed;
        if (travelMin + VISIT_TIME > remaining) break;
        pool.splice(bestIdx, 1);
        steps.push({ name: poi.name, type: poi.type, lat: poi.lat, lon: poi.lon });
        totalDist += bestDist;
        remaining -= travelMin + VISIT_TIME;
        curLat = poi.lat; curLon = poi.lon; curElev = poi.elev || 0;
    }
    return { steps, totalDist: Math.round(totalDist), usedMin: Math.round(durationMin - remaining) };
}

// WMO weather code → libellé français
function weatherLabel(code) {
    if (code === 0) return "Ensoleillé";
    if (code <= 2) return "Peu nuageux";
    if (code === 3) return "Nuageux";
    if (code <= 48) return "Brouillard";
    if (code <= 57) return "Bruine";
    if (code <= 67) return "Pluie";
    if (code <= 77) return "Neige";
    if (code <= 82) return "Averses";
    return "Orage";
}

async function getWeatherForecast(lat, lon) {
    try {
        const resp = await axios.get('https://api.open-meteo.com/v1/forecast', {
            params: {
                latitude: lat,
                longitude: lon,
                daily: 'weathercode,precipitation_sum',
                timezone: 'auto',
                forecast_days: 7
            },
            timeout: 5000
        });
        const { time, weathercode, precipitation_sum } = resp.data.daily;
        const today = new Date().toISOString().slice(0, 10);
        const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10);
        const DAYS_FR = ['Dim.', 'Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.'];
        const MONTHS_FR = ['jan.', 'fév.', 'mars', 'avr.', 'mai', 'juin', 'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.'];
        return time.map((date, i) => {
            const d = new Date(date + 'T12:00:00');
            let label;
            if (date === today) label = "Aujourd'hui";
            else if (date === tomorrow) label = "Demain";
            else label = `${DAYS_FR[d.getDay()]} ${d.getDate()} ${MONTHS_FR[d.getMonth()]}`;
            const code = weathercode[i];
            const precip = precipitation_sum[i] || 0;
            return {
                date,
                label,
                condition: weatherLabel(code),
                isGood: code <= 2 && precip < 1
            };
        });
    } catch {
        return null;
    }
}

/**
 * [POST] /api/itineraries/generate
 * Body: { locations:[{lat,lon,name}], durationMinutes, mode, activities:[String], wantsGoodWeather?:Boolean }
 */
app.post('/api/itineraries/generate', async (req, res) => {
    try {
        const { locations, durationMinutes, mode, activities, wantsGoodWeather } = req.body;
        if (!locations || locations.length === 0) {
            return res.status(400).json({ error: "Au moins un lieu requis" });
        }

        const startLat = locations[0].lat;
        const startLon = locations[0].lon;
        const duration = parseInt(durationMinutes) || 60;
        const speed = MODE_SPEED[mode] || MODE_SPEED.walking;
        const radius = Math.min(speed * (duration / 2), 2000);

        const activityFilters = [];
        if (!activities || activities.length === 0 || activities.includes('Restauration')) {
            activityFilters.push(`node["amenity"~"restaurant|cafe|bar|bakery|fast_food"](around:${radius},${startLat},${startLon});`);
        }
        if (!activities || activities.length === 0 || activities.includes('Culture')) {
            activityFilters.push(`node["tourism"~"museum|gallery|attraction|monument"](around:${radius},${startLat},${startLon});`);
        }
        if (!activities || activities.length === 0 || activities.includes('Loisirs')) {
            activityFilters.push(`node["leisure"~"park|garden|playground"](around:${radius},${startLat},${startLon});`);
            activityFilters.push(`node["amenity"~"cinema|theatre|library"](around:${radius},${startLat},${startLon});`);
        }

        if (activityFilters.length === 0) {
            return res.status(400).json({ error: "Aucune catégorie sélectionnée" });
        }

        const query = `[out:json][timeout:15];(${activityFilters.join('')});out body;`;
        const params = new URLSearchParams({ data: query });
        const osmRes = await axios.post('https://overpass-api.de/api/interpreter', params.toString(), {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'User-Agent': 'TravelingApp/1.0 (contact@traveling.app)'
            },
            timeout: 20000
        });

        const pois = (osmRes.data.elements || [])
            .filter(el => el.tags?.name && el.lat && el.lon)
            .map(el => ({
                name: el.tags.name,
                type: el.tags.amenity || el.tags.tourism || el.tags.leisure || 'lieu',
                lat: el.lat,
                lon: el.lon,
                category: el.tags.amenity && (el.tags.amenity.includes('restaurant') || el.tags.amenity.includes('cafe') || el.tags.amenity.includes('bar') || el.tags.amenity.includes('food'))
                    ? 'Restauration'
                    : el.tags.tourism ? 'Culture' : 'Loisirs'
            }));

        if (pois.length === 0) {
            return res.status(404).json({ error: "Aucun lieu trouvé dans cette zone. Essayez un rayon plus large ou d'autres activités." });
        }

        // Récupérer les altitudes pour tous les POIs
        const poisCoords = pois.map(p => [p.lat, p.lon]);
        const elevations = await getElevations(poisCoords);
        pois.forEach((p, i) => { p.elev = elevations[i] || 0; });

        // Variante 1 – Découverte : tous les POIs, greedy nearest-neighbor
        const v1 = buildRoute(pois, startLat, startLon, 0, duration, speed);

        // Variante 2 – Thématique : catégorie principale en priorité
        const primaryCategory = (activities && activities.length > 0) ? activities[0] : 'Restauration';
        const primaryPois = pois.filter(p => p.category === primaryCategory);
        const secondaryPois = pois.filter(p => p.category !== primaryCategory);
        const v2 = buildRoute(
            primaryPois.length > 0 ? [...primaryPois, ...secondaryPois] : pois,
            startLat, startLon, 0, duration, speed
        );

        // Variante 3 – Panoramique : max 2 POIs par catégorie (variété forcée)
        const byCategory = {};
        for (const p of pois) {
            if (!byCategory[p.category]) byCategory[p.category] = [];
            byCategory[p.category].push(p);
        }
        const diversePool = Object.values(byCategory).flatMap(arr => arr.slice(0, 2));
        const v3 = buildRoute(diversePool, startLat, startLon, 0, duration, speed);

        const toVariant = (name, description, route) => ({
            name,
            description,
            estimatedDuration: route.usedMin,
            estimatedDistance: route.totalDist,
            steps: route.steps
        });

        const variants = [
            toVariant("Découverte", "Un mix équilibré de lieux à visiter autour de vous", v1),
            toVariant("Thématique", `Centré sur ${primaryCategory.toLowerCase()} avec quelques extras`, v2),
            toVariant("Panoramique", "Un tour varié avec un lieu par catégorie", v3)
        ].filter(v => v.steps.length > 0);

        if (variants.length === 0) {
            return res.status(404).json({ error: "Pas assez de temps ou de lieux pour créer un itinéraire" });
        }

        console.log(`✅ ${variants.length} variante(s) générée(s) à partir de ${pois.length} lieux`);

        let goodWeatherDays = null;
        if (wantsGoodWeather) {
            goodWeatherDays = await getWeatherForecast(startLat, startLon);
            console.log(`☀️  Météo récupérée : ${goodWeatherDays?.length ?? 0} jours`);
        }

        res.json({ variants, goodWeatherDays });

    } catch (error) {
        if (error.response) {
            console.error("💥 Erreur Overpass:", error.response.status, error.response.data?.substring?.(0, 200));
        } else {
            console.error("💥 Erreur génération itinéraire:", error.message);
        }
        res.status(500).json({ error: "Erreur lors de la génération de l'itinéraire" });
    }
});

/**
 * [POST] /api/itineraries/save
 * Body: { userId, name, duration, distance, mode, steps:[{name,type,lat,lon}] }
 */
app.post('/api/itineraries/save', async (req, res) => {
    try {
        const { userId, name, duration, distance, mode, steps } = req.body;
        if (!userId || !name || !steps) {
            return res.status(400).json({ error: "Champs manquants" });
        }
        const itinerary = await prisma.itinerary.create({
            data: {
                name,
                duration: parseInt(duration) || 0,
                distance: parseFloat(distance) || 0,
                mode: mode || 'walking',
                authorId: parseInt(userId),
                steps: {
                    create: steps.map((s, i) => ({
                        order: i,
                        name: s.name,
                        type: s.type || null,
                        latitude: s.lat,
                        longitude: s.lon
                    }))
                }
            },
            include: { steps: { orderBy: { order: 'asc' } } }
        });
        console.log(`✅ Itinéraire "${name}" sauvegardé (${steps.length} étapes)`);
        res.status(201).json(itinerary);
    } catch (error) {
        console.error("💥 Erreur sauvegarde itinéraire:", error);
        res.status(500).json({ error: "Erreur lors de la sauvegarde" });
    }
});

/**
 * [GET] /api/users/:userId/itineraries
 */
app.get('/api/users/:userId/itineraries', async (req, res) => {
    try {
        const { userId } = req.params;
        const itineraries = await prisma.itinerary.findMany({
            where: { authorId: parseInt(userId) },
            include: { _count: { select: { steps: true } } },
            orderBy: { createdAt: 'desc' }
        });
        res.json(itineraries.map(it => ({
            id: it.id,
            name: it.name,
            duration: it.duration,
            distance: it.distance,
            mode: it.mode,
            createdAt: it.createdAt,
            stepsCount: it._count.steps
        })));
    } catch (error) {
        console.error("💥 Erreur récupération itinéraires:", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});

/**
 * [PATCH] /api/itineraries/:id
 * Met à jour l'ordre/contenu des étapes d'un itinéraire existant
 * Body: { name, steps:[{name,type,latitude,longitude}] }
 */
app.patch('/api/itineraries/:id', async (req, res) => {
    try {
        const { id } = req.params;
        const { name, steps } = req.body;
        if (!steps) return res.status(400).json({ error: "steps requis" });

        await prisma.itineraryStep.deleteMany({ where: { itineraryId: parseInt(id) } });

        const itinerary = await prisma.itinerary.update({
            where: { id: parseInt(id) },
            data: {
                ...(name && { name }),
                steps: {
                    create: steps.map((s, i) => ({
                        order: i,
                        name: s.name,
                        type: s.type || null,
                        latitude: s.latitude,
                        longitude: s.longitude
                    }))
                }
            },
            include: { steps: { orderBy: { order: 'asc' } } }
        });

        console.log(`✅ Itinéraire ${id} mis à jour (${steps.length} étapes)`);
        res.json(itinerary);
    } catch (error) {
        console.error("💥 Erreur mise à jour itinéraire:", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});

/**
 * [GET] /api/itineraries/:id
 * Retourne l'itinéraire complet avec toutes ses étapes (lat/lon inclus)
 */
app.get('/api/itineraries/:id', async (req, res) => {
    try {
        const { id } = req.params;
        const itinerary = await prisma.itinerary.findUnique({
            where: { id: parseInt(id) },
            include: { steps: { orderBy: { order: 'asc' } } }
        });
        if (!itinerary) return res.status(404).json({ error: "Itinéraire non trouvé" });
        res.json(itinerary);
    } catch (error) {
        console.error("💥 Erreur récupération itinéraire:", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});

/**
 * [GET] /api/route/multi
 * Calcule un trajet multi-étapes via OSRM
 * Query: waypoints=lat1,lon1;lat2,lon2;... & mode=walking|jogging|driving
 */
app.get('/api/route/multi', async (req, res) => {
    try {
        const { waypoints, mode } = req.query;
        if (!waypoints) return res.status(400).json({ error: "waypoints requis" });

        const profile = (mode === 'jogging') ? 'walking' : (mode || 'walking');
        const waypointStr = waypoints.split(';').map(w => {
            const [lat, lon] = w.split(',');
            return `${parseFloat(lon)},${parseFloat(lat)}`;
        }).join(';');

        const osrmUrl = `https://router.project-osrm.org/route/v1/${profile}/${waypointStr}?overview=full&geometries=geojson`;
        const response = await axios.get(osrmUrl, {
            headers: { 'User-Agent': 'TravelingApp/1.0' },
            timeout: 15000
        });

        if (!response.data?.routes?.length) {
            return res.status(404).json({ error: "Aucune route trouvée" });
        }

        const route = response.data.routes[0];
        const points = route.geometry.coordinates.map(([lon, lat]) => ({ latitude: lat, longitude: lon }));

        res.json({
            points,
            distance: Math.round(route.distance),
            duration: Math.round(route.duration)
        });
    } catch (error) {
        console.error("💥 Erreur route multi:", error.message);
        res.status(500).json({ error: "Impossible de calculer le trajet" });
    }
});

/**
 * [POST] /api/itineraries/:id/share
 * Crée un post public lié à cet itinéraire
 * Body: { userId, description }
 *//**
 * [POST] /api/itineraries/:id/share
 * Crée un post lié à cet itinéraire (Public ou pour un Groupe)
 */
app.post('/api/itineraries/:id/share', async (req, res) => {
    try {
        const { id } = req.params;
        // On récupère isPublic et groupId envoyés par l'app
        const { userId, description, isPublic, groupId } = req.body;
        
        if (!userId) return res.status(400).json({ error: "userId requis" });

        const itinerary = await prisma.itinerary.findUnique({
            where: { id: parseInt(id) },
            include: { steps: { orderBy: { order: 'asc' } } }
        });
        
        if (!itinerary) return res.status(404).json({ error: "Itinéraire non trouvé" });

        const firstStep = itinerary.steps[0];
        
        // Création du post (photo) lié à l'itinéraire
        const photo = await prisma.photo.create({
            data: {
                url: '', // Pas d'image physique, le client affichera la preview de l'itinéraire
                description: description || `Itinéraire : ${itinerary.name}`,
                type_lieu: itinerary.name,
                latitude: firstStep?.latitude || 0,
                longitude: firstStep?.longitude || 0,
                // Utilisation des nouvelles valeurs
                is_public: isPublic === undefined ? true : isPublic, 
                groupId: groupId ? parseInt(groupId) : null,
                authorId: parseInt(userId),
                itineraryId: parseInt(id),
                likesCount: 0
            },
            include: { 
                author: true, 
                itinerary: { include: { steps: { orderBy: { order: 'asc' } } } } 
            }
        });

        console.log(`📤 Itinéraire "${itinerary.name}" partagé (Public: ${photo.is_public}, Group: ${photo.groupId})`);
        
        res.status(201).json({
            id: photo.id.toString(),
            title: photo.type_lieu,
            content: photo.description,
            itineraryId: photo.itineraryId,
            isPublic: photo.is_public,
            groupName: photo.group?.name || null,
            sharedItinerary: photo.itinerary ? {
                id: photo.itinerary.id,
                name: photo.itinerary.name,
                duration: photo.itinerary.duration,
                distance: photo.itinerary.distance,
                mode: photo.itinerary.mode,
                steps: photo.itinerary.steps.map(s => ({
                    id: s.id, order: s.order, name: s.name,
                    type: s.type, latitude: s.latitude, longitude: s.longitude
                }))
            } : null
        });
    } catch (error) {
        console.error("💥 Erreur partage itinéraire:", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});
/**
 * [POST] /api/itineraries/:id/copy
 * Copie un itinéraire dans la liste de l'utilisateur
 * Body: { userId }
 */
app.post('/api/itineraries/:id/copy', async (req, res) => {
    try {
        const { id } = req.params;
        const { userId } = req.body;
        if (!userId) return res.status(400).json({ error: "userId requis" });

        const original = await prisma.itinerary.findUnique({
            where: { id: parseInt(id) },
            include: { steps: { orderBy: { order: 'asc' } } }
        });
        if (!original) return res.status(404).json({ error: "Itinéraire non trouvé" });

        const copy = await prisma.itinerary.create({
            data: {
                name: `${original.name} (copié)`,
                duration: original.duration,
                distance: original.distance,
                mode: original.mode,
                authorId: parseInt(userId),
                steps: {
                    create: original.steps.map(s => ({
                        order: s.order, name: s.name, type: s.type,
                        latitude: s.latitude, longitude: s.longitude
                    }))
                }
            },
            include: { steps: true }
        });

        console.log(`📋 Itinéraire #${id} copié → #${copy.id} pour user ${userId}`);
        res.status(201).json({
            id: copy.id, name: copy.name, duration: copy.duration,
            distance: copy.distance, mode: copy.mode,
            stepsCount: copy.steps.length, createdAt: copy.createdAt
        });
    } catch (error) {
        console.error("💥 Erreur copie itinéraire:", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});

/**
 * [GET] /api/itineraries/:id/pdf
 * Génère et retourne un PDF de l'itinéraire
 */
app.get('/api/itineraries/:id/pdf', async (req, res) => {
    try {
        const { id } = req.params;
        const itinerary = await prisma.itinerary.findUnique({
            where: { id: parseInt(id) },
            include: { steps: { orderBy: { order: 'asc' } }, author: true }
        });
        if (!itinerary) return res.status(404).json({ error: "Itinéraire non trouvé" });

        const safeName = itinerary.name.replace(/[^a-z0-9]/gi, '_');
        res.setHeader('Content-Type', 'application/pdf');
        res.setHeader('Content-Disposition', `attachment; filename="itineraire_${safeName}.pdf"`);

        const doc = new PDFDocument({ margin: 50, size: 'A4' });
        doc.pipe(res);

        // En-tête
        doc.fontSize(22).fillColor('#5C3D91').text(itinerary.name, { align: 'center' });
        doc.moveDown(0.3);
        doc.fontSize(11).fillColor('#888888').text(
            `Créé par ${itinerary.author?.pseudo || itinerary.author?.username || 'Anonyme'}  •  ${new Date(itinerary.createdAt).toLocaleDateString('fr-FR')}`,
            { align: 'center' }
        );
        doc.moveDown(0.8);

        // Ligne de séparation
        doc.moveTo(50, doc.y).lineTo(545, doc.y).strokeColor('#CCCCCC').stroke();
        doc.moveDown(0.6);

        // Infos générales
        const modeLbl = itinerary.mode === 'jogging' ? 'Gros effort (jogging)' : 'Effort modéré (marche)';
        const distKm = (itinerary.distance / 1000).toFixed(1);
        doc.fontSize(12).fillColor('#333333');
        doc.text(`Durée estimée : ${itinerary.duration} min`, { continued: true });
        doc.text(`   •   Distance : ${distKm} km`, { continued: true });
        doc.text(`   •   Mode : ${modeLbl}`);
        doc.moveDown(1);

        // Étapes
        doc.fontSize(14).fillColor('#5C3D91').text('Étapes de l\'itinéraire', { underline: true });
        doc.moveDown(0.5);

        itinerary.steps.forEach((step, i) => {
            const isLast = i === itinerary.steps.length - 1;
            doc.fontSize(12).fillColor('#333333')
                .text(`${i + 1}.  ${step.name}`, { continued: !!step.type });
            if (step.type) {
                doc.fontSize(10).fillColor('#888888').text(`  (${step.type})`);
            }
            doc.fontSize(9).fillColor('#AAAAAA')
                .text(`       ${step.latitude.toFixed(5)}, ${step.longitude.toFixed(5)}`);
            if (!isLast) {
                doc.fontSize(9).fillColor('#BBBBBB').text('       ↓', { align: 'left' });
            }
            doc.moveDown(0.3);
        });

        doc.moveDown(1);
        doc.moveTo(50, doc.y).lineTo(545, doc.y).strokeColor('#CCCCCC').stroke();
        doc.moveDown(0.5);
        doc.fontSize(9).fillColor('#BBBBBB').text('Généré par PinkPath', { align: 'center' });

        doc.end();
    } catch (error) {
        console.error("💥 Erreur génération PDF:", error);
        if (!res.headersSent) res.status(500).json({ error: "Erreur génération PDF" });
    }
});

// Lancement
app.listen(PORT, '0.0.0.0', () => {
    console.log(`-------------------------------------------`);
    console.log(`🚀 Serveur Traveling opérationnel !`);
    console.log(`📡 URL API : ${BASE_URL}/api/photos`);
    console.log(`📁 Dossier uploads : OK`);
    console.log(`-------------------------------------------`);
});