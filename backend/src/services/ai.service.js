// ============================================
// RakshakMesh AI Prioritization Service
// ============================================

const getPeopleCount = (value) => {

    if (value === null || value === undefined) {
        return 0;
    }

    const text = String(value).trim().toLowerCase();

    // Extract all numbers from values such as:
    // "5"
    // "35-40"
    // "around 20"
    // "3 people"
    const numbers = text.match(/\d+/g);

    if (!numbers || numbers.length === 0) {
        return 0;
    }

    // For a range such as 35-40,
    // use the higher number for safer prioritization.
    return Math.max(
        ...numbers.map(Number)
    );
};


// ============================================
// KEYWORD CATEGORIES
// ============================================

const criticalKeywords = [

    // Life-threatening medical conditions
    "unconscious",
    "unresponsive",
    "not breathing",
    "unable to breathe",
    "can't breathe",
    "cannot breathe",
    "cannot breath",
    "can't breath",
    "breathing difficulty",
    "difficulty breathing",
    "breathing problem",
    "shortness of breath",
    "severe breathing",
    "choking",
    "heart attack",
    "cardiac arrest",
    "severe bleeding",
    "heavy bleeding",
    "bleeding heavily",
    "blood loss",
    "critical condition",
    "critical injury",
    "life threatening",
    "life-threatening",
    "dying",
    "dead",
    "death",
    "fatal",
    "serious injury",
    "severe injury",

    // Trapped / inaccessible
    "trapped",
    "stuck",
    "cannot escape",
    "can't escape",
    "unable to escape",
    "buried",
    "buried under",
    "crushed",
    "pinned",
    "collapsed on",
    "blocked",

    // Major structural disaster
    "building collapsed",
    "building collapse",
    "house collapsed",
    "roof collapsed",
    "bridge collapsed",
    "wall collapsed",
    "structure collapsed",
    "structural collapse",

    // Immediate danger
    "explosion",
    "exploded",
    "blast",
    "gas leak",
    "gas explosion",
    "electrocution",
    "electric shock"
];


const medicalKeywords = [

    "medical",
    "medical emergency",
    "injured",
    "injury",
    "injuries",
    "wounded",
    "wound",
    "bleeding",
    "blood",
    "fracture",
    "broken bone",
    "burn",
    "burns",
    "severe burns",
    "head injury",
    "head trauma",
    "trauma",
    "unconscious",
    "unresponsive",
    "breathing",
    "breathless",
    "breathlessness",
    "unable to breathe",
    "can't breathe",
    "cannot breathe",
    "breathing difficulty",
    "difficulty breathing",
    "breathing problem",
    "shortness of breath",
    "choking",
    "ambulance",
    "doctor",
    "hospital",
    "medicine",
    "medication",
    "first aid",
    "paramedic",
    "pregnant",
    "pregnancy",
    "child injured",
    "children injured",
    "elderly injured"
];


const fireKeywords = [

    "fire",
    "fire caught",
    "fire broke out",
    "fire outbreak",
    "burning",
    "flames",
    "flame",
    "smoke",
    "heavy smoke",
    "thick smoke",
    "huge smoke",
    "gas fire",
    "house fire",
    "building fire",
    "forest fire",
    "wildfire",
    "electrical fire",
    "kitchen fire",
    "chemical fire",
    "explosion",
    "blast"
];


const floodKeywords = [

    "flood",
    "flooded",
    "flooding",
    "flash flood",
    "flash flooding",
    "water level rising",
    "rising water",
    "heavy water",
    "water entered",
    "water inside",
    "submerged",
    "drowning",
    "drowned",
    "drowning people",
    "washed away",
    "swept away",
    "waterlogging",
    "waterlogged",
    "overflow",
    "river overflow",
    "dam overflow",
    "dam burst"
];


const earthquakeKeywords = [

    "earthquake",
    "earthquake hit",
    "earthquake struck",
    "tremor",
    "tremors",
    "seismic",
    "aftershock",
    "aftershocks",
    "building shaking",
    "ground shaking"
];


const landslideKeywords = [

    "landslide",
    "land slide",
    "mudslide",
    "mud slide",
    "rockslide",
    "rocks falling",
    "debris",
    "soil collapse",
    "mountain collapse"
];


const cycloneKeywords = [

    "cyclone",
    "hurricane",
    "typhoon",
    "storm",
    "severe storm",
    "tropical storm",
    "high winds",
    "strong winds",
    "wind damage",
    "trees fallen",
    "tree fallen"
];


const tsunamiKeywords = [

    "tsunami",
    "tidal wave",
    "sea water rising",
    "sea flooding",
    "coastal flooding",
    "coastal surge"
];


const rescueKeywords = [

    "rescue",
    "help",
    "urgent help",
    "emergency help",
    "send help",
    "need assistance",
    "need rescue",
    "send rescue",
    "evacuate",
    "evacuation",
    "evacuate people",
    "people trapped",
    "people stuck"
];


// ============================================
// KEYWORD MATCHING
// ============================================

const containsKeyword = (text, keywords) => {

    return keywords.some(
        keyword => text.includes(keyword)
    );

};


// ============================================
// MAIN AI PRIORITIZATION
// ============================================

const prioritizeSOS = (sos) => {

    const text = `
        ${sos.message || ""}
        ${sos.condition || ""}
        ${sos.type || ""}
    `
        .toLowerCase()
        .trim();


    const peopleCount = getPeopleCount(
        sos.peopleCount
    );


    // ----------------------------------------
    // Detect incident types
    // ----------------------------------------

    const isMedical = containsKeyword(
        text,
        medicalKeywords
    );

    const isFire = containsKeyword(
        text,
        fireKeywords
    );

    const isFlood = containsKeyword(
        text,
        floodKeywords
    );

    const isEarthquake = containsKeyword(
        text,
        earthquakeKeywords
    );

    const isLandslide = containsKeyword(
        text,
        landslideKeywords
    );

    const isCyclone = containsKeyword(
        text,
        cycloneKeywords
    );

    const isTsunami = containsKeyword(
        text,
        tsunamiKeywords
    );


    const isCritical = containsKeyword(
        text,
        criticalKeywords
    );


    const needsRescue = containsKeyword(
        text,
        rescueKeywords
    );


    // ----------------------------------------
    // Determine incident type
    // ----------------------------------------

    let incidentType = "GENERAL";

    if (isFire) {

        incidentType = "FIRE";

    } else if (isFlood) {

        incidentType = "FLOOD";

    } else if (isEarthquake) {

        incidentType = "EARTHQUAKE";

    } else if (isLandslide) {

        incidentType = "LANDSLIDE";

    } else if (isCyclone) {

        incidentType = "CYCLONE";

    } else if (isTsunami) {

        incidentType = "TSUNAMI";

    } else if (isMedical) {

        incidentType = "MEDICAL";
    }


    // ----------------------------------------
    // Determine priority
    // ----------------------------------------

    let priority = "P3";
    let severity = "LOW";


    // P0 — Critical / immediate threat
    if (
        isCritical ||
        peopleCount >= 10 ||
        (
            isFire &&
            (
                isMedical ||
                needsRescue ||
                peopleCount >= 5
            )
        ) ||
        (
            isFlood &&
            (
                text.includes("drowning") ||
                text.includes("drowned") ||
                text.includes("submerged") ||
                peopleCount >= 5
            )
        )
    ) {

        priority = "P0";
        severity = "CRITICAL";

    }

    // P1 — High
    else if (
        isMedical ||
        needsRescue ||
        peopleCount >= 3 ||
        isFire ||
        isFlood ||
        isEarthquake ||
        isLandslide ||
        isTsunami
    ) {

        priority = "P1";
        severity = "HIGH";

    }

    // P2 — Moderate
    else if (
        isCyclone ||
        text.includes("damage") ||
        text.includes("danger") ||
        text.includes("unsafe") ||
        text.includes("blocked") ||
        text.includes("power outage") ||
        text.includes("road blocked")
    ) {

        priority = "P2";
        severity = "MEDIUM";

    }


    // ----------------------------------------
    // Medical needs
    // ----------------------------------------

    const medicalNeeds =
        isMedical ||
        containsKeyword(text, [
            "injured",
            "injury",
            "bleeding",
            "burn",
            "burns",
            "unconscious",
            "unresponsive",
            "breathing",
            "ambulance",
            "hospital",
            "doctor",
            "medical"
        ]);


    // ----------------------------------------
    // Required resources
    // ----------------------------------------

    const requiredResources = [];

    if (medicalNeeds) {
        requiredResources.push(
            "MEDICAL_TEAM"
        );
    }

    if (isFire) {
        requiredResources.push(
            "FIRE_RESCUE"
        );
    }

    if (isFlood) {
        requiredResources.push(
            "WATER_RESCUE"
        );
    }

    if (
        isEarthquake ||
        isLandslide ||
        text.includes("building collapsed") ||
        text.includes("building collapse")
    ) {
        requiredResources.push(
            "SEARCH_AND_RESCUE"
        );
    }

    if (
        needsRescue ||
        isCritical
    ) {
        requiredResources.push(
            "RESCUE_TEAM"
        );
    }


    // ----------------------------------------
    // Final result
    // ----------------------------------------

    return {

        priority,

        severity,

        incidentType,

        medicalNeeds,

        requiredResources

    };

};


module.exports = {
    prioritizeSOS
};