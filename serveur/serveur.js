
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

const JWT_SECRET = "ton_secret_ultra_confidentiel"; // Change ça pour la prod
// 1. Configuration de la connexion PostgreSQL
const connectionString = process.env.DATABASE_URL;
const pool = new Pool({ connectionString });
const adapter = new PrismaPg(pool);
const prisma = new PrismaClient({ adapter });

const app = express();

// --- CONFIGURATION ---
// IMPORTANT : Remplace "0.0.0.0" ici par ton IP réelle (ex: 192.168.1.XX)
// pour que ton téléphone puisse charger les images !
const SERVER_IP = "10.139.5.174"//"192.168.1.25"; 
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
                likedBy: userId ? { where: { userId: userId } } : false
            },
            orderBy: { date: 'desc' }
        });

        const formattedPosts = photos.map(p => ({
            id: p.id.toString(),
            title: p.type_lieu,
            content: p.description,
            latitude: p.latitude,
            longitude: p.longitude,
           imageUrl: `${BASE_URL}${p.url}`,
            audioUrl: p.audioUrl ? `${BASE_URL}${p.audioUrl}` : null,             
            author: p.author ? (p.author.pseudo || p.author.username) : "Anonyme",
            authorAvatarUrl: p.author && p.author.profileUrl ? `${BASE_URL}${p.author.profileUrl}` : null,
            likes: p.likesCount || 0,
            commentCount: p._count ? p._count.comments : 0,
            isLiked: p.likedBy && p.likedBy.length > 0,
            tags: p.tags.map(t => t.name),
            groupName: p.group ? p.group.name : null,
            isPublic: p.is_public // Confirmé pour l'onglet "Populaires"
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
                                    select: { id: true, profileUrl: true }
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


/** [GET] Récupérer les détails d'un groupe (Membres + Photos) */
app.get('/api/groups/:groupId', async (req, res) => {
    try {
        const { groupId } = req.params;
        const group = await prisma.group.findUnique({
            where: { id: parseInt(groupId) },
            include: {
                users: {
                    select: {
                        id: true,
                        username: true,
                        pseudo: true,
                        profileUrl: true
                    }
                },
                photos: {
                    include: {
                        author: true,
                        tags: true,
                        _count: { select: { comments: true } }
                    },
                    orderBy: { date: 'desc' }
                }
            }
        });

        if (!group) return res.status(404).json({ error: "Groupe non trouvé" });

        // Formatage pour l'URL des images
        const formattedPhotos = group.photos.map(p => ({
            ...p,
            imageUrl: `${BASE_URL}${p.url}`,
            authorAvatarUrl: p.author?.profileUrl ? `${BASE_URL}${p.author.profileUrl}` : null
        }));

        res.json({
            ...group,
            photos: formattedPhotos
        });
    } catch (error) {
        res.status(500).json({ error: "Erreur serveur" });
    }
});



/** [POST] Ajouter un utilisateur à un groupe existant (via son pseudo) */
app.post('/api/groups/:groupId/add-user', async (req, res) => {
    try {
        const { groupId } = req.params;
        const { usernameToAdd } = req.body;

        const updatedGroup = await prisma.group.update({
            where: { id: parseInt(groupId) },
            data: {
                users: {
                    connect: { username: usernameToAdd }
                }
            },
            include: { users: true }
        });

        res.json({ message: `${usernameToAdd} a rejoint le groupe !`, group: updatedGroup });
    } catch (error) {
        console.error("💥 Erreur ajout membre :", error);
        res.status(404).json({ error: "Utilisateur ou groupe introuvable" });
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
    /** * [GET] /api/places/nearby
 * Récupère les lieux autour de l'utilisateur en temps réel depuis OpenStreetMap
 */
app.get('/api/places/nearby', async (req, res) => {
    try {
        const { lat, lon } = req.query;

        if (!lat || !lon) {
            return res.status(400).json({ error: "Latitude et Longitude manquantes" });
        }

        // On définit un rayon de 3km autour de l'utilisateur (3000 mètres)
        const radius = 3000;
        const overpassUrl = `https://overpass-api.de/api/interpreter`;
        
        // Requête Overpass : On cherche les lieux avec un nom (commerces, parcs, tourisme...)
        const overpassQuery = `
            [out:json][timeout:25];
            (
              node["name"](around:${radius},${lat},${lon});
              way["name"](around:${radius},${lat},${lon});
            );
            out center;
        `;

        const response = await axios.post(overpassUrl, overpassQuery);
        
        // On formate les données pour ton application mobile
        const places = response.data.elements.map(el => ({
            id: el.id.toString(),
            name: el.tags.name,
            type: el.tags.amenity || el.tags.tourism || el.tags.leisure || "Lieu d'intérêt",
            latitude: el.lat || el.center.lat,
            longitude: el.lon || el.center.lon,
        }));

        console.log(`📍 ${places.length} lieux trouvés autour de ${lat},${lon}`);
        res.json(places);

    } catch (error) {
        console.error("💥 Erreur OSM :", error);
        res.status(500).json({ error: "Erreur lors de la récupération des lieux" });
    }
});
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






// Lancement
app.listen(PORT, '0.0.0.0', () => {
    console.log(`-------------------------------------------`);
    console.log(`🚀 Serveur Traveling opérationnel !`);
    console.log(`📡 URL API : ${BASE_URL}/api/photos`);
    console.log(`📁 Dossier uploads : OK`);
    console.log(`-------------------------------------------`);
});