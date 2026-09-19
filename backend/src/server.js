require("dotenv").config();

console.log("MONGO_URI loaded:", !!process.env.MONGO_URI);

const app = require("./app");
const connectDB = require("./config/db");

const PORT = process.env.PORT || 5000;

const startServer = async () => {
    await connectDB();

    app.listen(PORT, "0.0.0.0", () => {
        console.log(`🚀 RakshakMesh Backend running on port ${PORT}`);
    });
};

startServer();