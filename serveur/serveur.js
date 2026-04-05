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
const SERVER_IP = "192.168.1.25"; 
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
    console.log("\n📥 [POST] Nouvelle tentative de publication...");

    try {
        const { description, type_lieu, latitude, longitude, is_public, authorId } = req.body;

        if (!req.files || !req.files['image']) {
            console.log("❌ Erreur : Image manquante.");
            return res.status(400).json({ error: "Image manquante" });
        }

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
                likesCount: 0
            }
        });

        console.log(`✅ Succès ! Post enregistré (ID: ${photo.id})`);
        res.status(201).json(photo);
    } catch (error) {
        console.error("💥 Erreur upload :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});


app.post('/api/auth/register', async (req, res) => {
    console.log("👤 Tentative de création de compte...");
    try {
        const { username, password } = req.body;
        console.log(`Détails reçus : username=${username}`);

        const hashedPassword = await bcrypt.hash(password, 10);
        console.log("Hashage réussi...");

        const user = await prisma.user.create({
            data: { username, password: hashedPassword }
        });

        console.log(`✅ Utilisateur créé : ${username}`);
        res.status(201).json({ userId: user.id });

    } catch (error) {
        // C'EST CETTE LIGNE QUI VA NOUS DIRE LA VÉRITÉ :
        console.error("❌ ERREUR DÉTAILLÉE :", error); 
        
        res.status(500).json({ 
            error: "Erreur serveur", 
            message: error.message,
            code: error.code 
        });
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

        // 3. GÉNÉRER LE TOKEN (C'est ce qui manquait !)
        const token = jwt.sign(
            { userId: user.id, username: user.username }, 
            JWT_SECRET, 
            { expiresIn: '24h' }
        );

        console.log(`🔓 Connexion réussie pour : ${username}`);

        // 4. Envoyer la réponse
        res.json({ 
            message: "Connexion réussie",
            token: token, // Maintenant la variable existe !
            user: { 
                id: user.id, 
                username: user.username,
                profileUrl: user.profileUrl ? `${BASE_URL}${user.profileUrl}` : null
            }
        });

    } catch (error) {
        console.error("💥 Erreur Login :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});


app.get('/api/photos', async (req, res) => {
    // On récupère l'ID de l'utilisateur connecté envoyé par Android
    // Exemple d'appel : /api/photos?userId=1
    const userId = req.query.userId ? parseInt(req.query.userId) : null;

    try {
        const photos = await prisma.photo.findMany({
            where: { is_public: true },
			take: 20,
            include: { 
                author: true,
                _count: { select: { comments: true } },
                // ON CHARGE LE LIKE de l'utilisateur actuel s'il existe
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
            author: p.author ? p.author.username : "Anonyme",
            authorAvatarUrl: p.author && p.author.profileUrl ? `${BASE_URL}${p.author.profileUrl}` : null,
            likes: p.likesCount || 0,
            commentCount: p._count ? p._count.comments : 0,

            // --- LA LOGIQUE DU CŒUR EST ICI ---
            // Si likedBy contient un élément, ça veut dire que l'utilisateur a liké !
            isLiked: p.likedBy && p.likedBy.length > 0 
        }));

        res.json(formattedPosts);
    } catch (error) {
        console.error("💥 Erreur flux :", error);
        res.status(500).json({ error: "Erreur serveur" });
    }
});


app.get('/api/photos/:photoId/comments', async (req, res) => {
    try {
        const { photoId } = req.params;
        const comments = await prisma.comment.findMany({
            where: { photoId: parseInt(photoId) },
            include: { 
                author: true // Récupère les infos de celui qui a commenté
            },
            orderBy: { createdAt: 'asc' }
        });

        // On formate pour que l'app ait des URLs utilisables (http://...)
        const formattedComments = comments.map(c => ({
            id: c.id,
            text: c.text,
            date: c.createdAt,
            authorName: c.author.username,
            // URL de la photo de profil de celui qui commente
            authorAvatarUrl: c.author.profileUrl ? `${BASE_URL}${c.author.profileUrl}` : null
        }));

        res.json(formattedComments);
    } catch (error) {
        res.status(500).json({ error: "Erreur récupération commentaires" });
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
        res.json(comments);
    } catch (error) {
        res.status(500).json({ error: "Erreur lors de la récupération des commentaires" });
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








// Lancement
app.listen(PORT, '0.0.0.0', () => {
    console.log(`-------------------------------------------`);
    console.log(`🚀 Serveur Traveling opérationnel !`);
    console.log(`📡 URL API : ${BASE_URL}/api/photos`);
    console.log(`📁 Dossier uploads : OK`);
    console.log(`-------------------------------------------`);
});