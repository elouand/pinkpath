const express = require('express');
const { PrismaClient } = require('@prisma/client');
const multer = require('multer');
const path = require('path');
const cors = require('cors');

const prisma = new PrismaClient();
const app = express();

app.use(cors());
app.use(express.json());
// Pour servir les photos et audios statiquement
app.use('/uploads', express.static('uploads'));

// Configuration de stockage pour Multer
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, 'uploads/');
  },
  filename: (req, file, cb) => {
    cb(null, Date.now() + path.extname(file.originalname));
  }
});


app.post('/api/photos', upload.fields([{ name: 'image', maxCount: 1 }, { name: 'audio', maxCount: 1 }]), async (req, res) => {
  try {
    const { description, type_lieu, latitude, longitude, is_public, authorId, groupId } = req.body;
    
    const photo = await prisma.photo.create({
      data: {
        url: `/uploads/${req.files['image'][0].filename}`,
        audioUrl: req.files['audio'] ? `/uploads/${req.files['audio'][0].filename}` : null, 
        description, 
        type_lieu,
        latitude: parseFloat(latitude), 
        longitude: parseFloat(longitude),
        is_public: is_public === 'true',
        authorId: parseInt(authorId),
        groupId: groupId ? parseInt(groupId) : null 
      }
    });
    res.json(photo);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});



const upload = multer({ storage: storage });

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Serveur Traveling lancé sur le port ${PORT}`);
});