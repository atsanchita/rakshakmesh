const express = require("express");

const {
    receiveSOS,
    getAllSOS
} = require("../controllers/sos.controller");

const router = express.Router();

router.post("/", receiveSOS);

router.get("/", getAllSOS);

module.exports = router;