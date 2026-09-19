const mongoose = require("mongoose");

const sosSchema = new mongoose.Schema(
    {
        sosId: {
            type: String,
            required: true,
            unique: true,
            index: true
        },

        message: {
            type: String,
            default: ""
        },

        peopleCount: {
            type: String,
            default: ""
        },

        condition: {
            type: String,
            default: ""
        },

        latitude: {
            type: Number,
            default: null
        },

        longitude: {
            type: Number,
            default: null
        },

        timestamp: {
            type: String,
            default: null
        },

        status: {
            type: String,
            default: "PENDING"
        },

        // =========================
        // AI CLASSIFICATION
        // =========================

        priority: {
            type: String,
            default: "P3"
        },

        severity: {
            type: String,
            default: "LOW"
        },

        incidentType: {
            type: String,
            default: "GENERAL"
        },

        medicalNeeds: {
            type: Boolean,
            default: false
        },

        requiredResources: {
            type: [String],
            default: []
        },

        // =========================
        // SERVER METADATA
        // =========================

        receivedAt: {
            type: Date,
            default: Date.now
        }
    },
    {
        timestamps: true
    }
);

const SOS = mongoose.model("SOS", sosSchema);

module.exports = SOS;