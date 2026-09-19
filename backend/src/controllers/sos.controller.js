const sosService = require("../services/sos.service");

// Receive SOS from Gateway
const receiveSOS = async (req, res, next) => {

    try {

        const sos = req.body;

        console.log("🚨 SOS RECEIVED:");
        console.log(sos);

        const result = await sosService.createSOS(sos);

        if (result.existing) {

            return res.status(200).json({
                success: true,
                message: "SOS already exists",
                sosId: result.sos.sosId
            });
        }

        console.log("🗄️ SOS stored in MongoDB");

        res.status(201).json({
            success: true,
            message: "SOS stored successfully",
            sosId: result.sos.sosId
        });

    } catch (error) {

        next(error);

    }
};

// Get all SOS
const getAllSOS = async (req, res, next) => {

    try {

        const reports = await sosService.getAllSOS();

        res.json({
            success: true,
            count: reports.length,
            reports
        });

    } catch (error) {

        next(error);

    }
};

module.exports = {
    receiveSOS,
    getAllSOS
};