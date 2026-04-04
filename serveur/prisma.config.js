// prisma.config.js
require('dotenv').config(); // <--- INDISPENSABLE pour lire le fichier .env

module.exports = {
  datasource: {
    url: process.env.DATABASE_URL,
  },
};