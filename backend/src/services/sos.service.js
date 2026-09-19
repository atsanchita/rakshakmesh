const SOS = require("../models/sos.model");
const { prioritizeSOS } = require("./ai.service");

const createSOS = async (sosData) => {

    const existingSOS = await SOS.findOne({
        sosId: sosData.sosId
    });

    if (existingSOS) {

        return {
            existing: true,
            sos: existingSOS
        };

    }

    // AI prioritization
    const aiResult = prioritizeSOS(sosData);

    console.log("🤖 AI PRIORITIZATION:");
    console.log(aiResult);

    const sos = await SOS.create({
        ...sosData,
        ...aiResult
    });

    return {
        existing: false,
        sos
    };
};

const getAllSOS = async () => {

    return await SOS
        .find()
        .sort({ createdAt: -1 });

};

module.exports = {
    createSOS,
    getAllSOS
};