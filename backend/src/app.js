const express = require("express");
const cors = require("cors");

const sosRoutes = require("./routes/sos.routes");
const errorHandler = require("./middleware/error.middleware");

const app = express();

app.use(cors());
app.use(express.json());

// Health check
app.get("/", (req, res) => {

    res.json({
        message: "RakshakMesh Backend is running 🚨"
    });

});

// API routes
app.use("/api/v1/sos", sosRoutes);

// Error handler
app.use(errorHandler);

module.exports = app;