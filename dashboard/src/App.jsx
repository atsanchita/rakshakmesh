import { useEffect, useMemo, useState } from "react";

import {
    MapContainer,
    TileLayer,
    Marker,
    Popup,
    useMap
} from "react-leaflet";

import L from "leaflet";

import "leaflet/dist/leaflet.css";
import "./App.css";


const API_URL = `${import.meta.env.VITE_API_URL}/api/v1/sos`;


// ============================================================
// LEAFLET DEFAULT ICON FIX
// ============================================================

delete L.Icon.Default.prototype._getIconUrl;

L.Icon.Default.mergeOptions({
    iconRetinaUrl:
        "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png",

    iconUrl:
        "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png",

    shadowUrl:
        "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png"
});


// ============================================================
// PRIORITY MARKER
// ============================================================

const createPriorityIcon = (priority) => {

    const config = {

        P0: {
            symbol: "🚨",
            className: "marker-p0"
        },

        P1: {
            symbol: "⚠️",
            className: "marker-p1"
        },

        P2: {
            symbol: "🟡",
            className: "marker-p2"
        },

        P3: {
            symbol: "🔵",
            className: "marker-p3"
        }

    };

    const current = config[priority] || config.P3;

    return L.divIcon({

        className: "custom-rescue-marker",

        html: `
            <div class="rescue-marker ${current.className}">
                ${current.symbol}
            </div>
        `,

        iconSize: [42, 42],

        iconAnchor: [21, 21],

        popupAnchor: [0, -20]

    });

};


// ============================================================
// MAP AUTO CENTER
// ============================================================

function MapController({ reports }) {

    const map = useMap();

    useEffect(() => {

        if (!reports.length) return;

        const bounds = L.latLngBounds(
            reports.map(report => [
                Number(report.latitude),
                Number(report.longitude)
            ])
        );

        if (reports.length === 1) {

            map.setView(
                [
                    Number(reports[0].latitude),
                    Number(reports[0].longitude)
                ],
                14
            );

        } else {

            map.fitBounds(bounds, {
                padding: [40, 40]
            });

        }

    }, [reports, map]);

    return null;
}


// ============================================================
// MAIN APP
// ============================================================

function App() {

    const [reports, setReports] = useState([]);

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");

    const [search, setSearch] = useState("");

    const [priorityFilter, setPriorityFilter] =
        useState("ALL");

    const [statusFilter, setStatusFilter] =
        useState("ALL");

    const [lastUpdated, setLastUpdated] =
        useState(null);


    // ========================================================
    // FETCH REPORTS
    // ========================================================

    const fetchReports = async () => {

        try {

            const response = await fetch(API_URL);

            if (!response.ok) {

                throw new Error(
                    "Failed to fetch SOS reports"
                );

            }

            const data = await response.json();

            setReports(data.reports || []);

            setError("");

            setLastUpdated(new Date());

        } catch (err) {

            console.error(err);

            setError(
                "Unable to connect to RakshakMesh backend"
            );

        } finally {

            setLoading(false);

        }

    };


    // ========================================================
    // INITIAL FETCH + AUTO REFRESH
    // ========================================================

    useEffect(() => {

        fetchReports();

        const interval = setInterval(
            fetchReports,
            5000
        );

        return () => clearInterval(interval);

    }, []);


    // ========================================================
    // PRIORITY COUNTS
    // ========================================================

    const countPriority = (priority) => {

        return reports.filter(
            report =>
                (report.priority || "P3") === priority
        ).length;

    };


    // ========================================================
    // STATUS
    // ========================================================

    const getStatus = (report) => {

        const savedStatuses =
            JSON.parse(
                localStorage.getItem(
                    "rakshakmesh_statuses"
                ) || "{}"
            );

        return (
            savedStatuses[report.sosId] ||
            report.status ||
            "PENDING"
        );

    };


    const updateStatus = (sosId, newStatus) => {

        const savedStatuses =
            JSON.parse(
                localStorage.getItem(
                    "rakshakmesh_statuses"
                ) || "{}"
            );

        savedStatuses[sosId] = newStatus;

        localStorage.setItem(
            "rakshakmesh_statuses",
            JSON.stringify(savedStatuses)
        );

        setReports(previousReports =>
            previousReports.map(report =>
                report.sosId === sosId
                    ? {
                        ...report,
                        status: newStatus
                    }
                    : report
            )
        );

    };


    // ========================================================
    // PRIORITY CLASS
    // ========================================================

    const getPriorityClass = (priority) => {

        switch (priority) {

            case "P0":
                return "critical";

            case "P1":
                return "high";

            case "P2":
                return "medium";

            default:
                return "low";

        }

    };


    // ========================================================
    // TIME FORMAT
    // ========================================================

    const formatTime = (timestamp) => {

        if (!timestamp) {
            return "Unknown";
        }

        return new Date(
            timestamp
        ).toLocaleString();

    };


    // ========================================================
    // VALID MAP REPORTS
    // ========================================================

    const mappedReports = reports.filter(report => {

        const latitude =
            Number(report.latitude);

        const longitude =
            Number(report.longitude);

        return (
            Number.isFinite(latitude) &&
            Number.isFinite(longitude)
        );

    });


    // ========================================================
    // FILTER REPORTS
    // ========================================================

    const filteredReports = useMemo(() => {

        return reports.filter(report => {

            const priority =
                report.priority || "P3";

            const status =
                getStatus(report);

            const searchText =
                search.toLowerCase();

            const searchableText = [

                report.message,

                report.incidentType,

                report.severity,

                report.sosId,

                report.condition

            ]
                .filter(Boolean)
                .join(" ")
                .toLowerCase();


            const matchesSearch =
                searchableText.includes(
                    searchText
                );


            const matchesPriority =
                priorityFilter === "ALL" ||
                priority === priorityFilter;


            const matchesStatus =
                statusFilter === "ALL" ||
                status === statusFilter;


            return (
                matchesSearch &&
                matchesPriority &&
                matchesStatus
            );

        });

    }, [
        reports,
        search,
        priorityFilter,
        statusFilter
    ]);


    // ========================================================
    // MAP REPORTS AFTER FILTER
    // ========================================================

    const filteredMappedReports =
        filteredReports.filter(report => {

            const latitude =
                Number(report.latitude);

            const longitude =
                Number(report.longitude);

            return (
                Number.isFinite(latitude) &&
                Number.isFinite(longitude)
            );

        });


    // ========================================================
    // MAP CENTER
    // ========================================================

const mapCenter = [19.0760, 72.8777];


    // ========================================================
    // ACTIVE COUNTS
    // ========================================================

    const pendingCount =
        reports.filter(
            report =>
                getStatus(report) === "PENDING"
        ).length;


    const acknowledgedCount =
        reports.filter(
            report =>
                getStatus(report) === "ACKNOWLEDGED"
        ).length;


    const dispatchedCount =
        reports.filter(
            report =>
                getStatus(report) === "DISPATCHED"
        ).length;


    const resolvedCount =
        reports.filter(
            report =>
                getStatus(report) === "RESOLVED"
        ).length;


    // ========================================================
    // UI
    // ========================================================

    return (

        <div className="dashboard">


            {/* ==================================================
                HEADER
            ================================================== */}

            <header className="header">

                <div>

                    <h1>
                        🚨 RakshakMesh
                    </h1>

                    <p>
                        Rescue Command Dashboard
                    </p>

                </div>


                <div className="live-status">

                    <span className="status-dot"></span>

                    LIVE

                </div>

            </header>


            {/* ==================================================
                ERROR
            ================================================== */}

            {error && (

                <div className="error-banner">

                    ⚠️ {error}

                </div>

            )}


            {/* ==================================================
                LAST UPDATED
            ================================================== */}

            <div className="update-bar">

                <span>

                    🛰️
                    Network monitoring active

                </span>

                <span>

                    🕒 Last updated:

                    {" "}

                    {lastUpdated
                        ? lastUpdated.toLocaleTimeString()
                        : "Loading..."}

                </span>

            </div>


            {/* ==================================================
                STATISTICS
            ================================================== */}

            <section className="stats">


                <div className="stat-card total">

                    <span>
                        Total SOS
                    </span>

                    <strong>
                        {reports.length}
                    </strong>

                </div>


                <div className="stat-card critical">

                    <span>
                        🚨 P0 Critical
                    </span>

                    <strong>
                        {countPriority("P0")}
                    </strong>

                </div>


                <div className="stat-card high">

                    <span>
                        ⚠️ P1 High
                    </span>

                    <strong>
                        {countPriority("P1")}
                    </strong>

                </div>


                <div className="stat-card medium">

                    <span>
                        🟡 P2 Medium
                    </span>

                    <strong>
                        {countPriority("P2")}
                    </strong>

                </div>


                <div className="stat-card low">

                    <span>
                        🔵 P3 Low
                    </span>

                    <strong>
                        {countPriority("P3")}
                    </strong>

                </div>


            </section>


            {/* ==================================================
                RESPONSE STATUS
            ================================================== */}

            <section className="stats">


                <div className="stat-card">

                    <span>
                        ⏳ Pending
                    </span>

                    <strong>
                        {pendingCount}
                    </strong>

                </div>


                <div className="stat-card">

                    <span>
                        👁️ Acknowledged
                    </span>

                    <strong>
                        {acknowledgedCount}
                    </strong>

                </div>


                <div className="stat-card">

                    <span>
                        🚑 Dispatched
                    </span>

                    <strong>
                        {dispatchedCount}
                    </strong>

                </div>


                <div className="stat-card">

                    <span>
                        ✅ Resolved
                    </span>

                    <strong>
                        {resolvedCount}
                    </strong>

                </div>


            </section>


            {/* ==================================================
                MAP
            ================================================== */}

            <section className="map-section">


                <div className="section-header">

                    <div>

                        <h2>
                            🗺️ Emergency Locations
                        </h2>

                        <p>
                            Live SOS locations received by
                            the rescue center
                        </p>

                    </div>


                    <span className="map-count">

                        📍
                        {" "}
                        {mappedReports.length}
                        {" "}
                        mapped

                    </span>

                </div>


                {/* MAP LEGEND */}

                <div className="map-legend">

                    <span>
                        🚨 P0 Critical
                    </span>

                    <span>
                        ⚠️ P1 High
                    </span>

                    <span>
                        🟡 P2 Medium
                    </span>

                    <span>
                        🔵 P3 Low
                    </span>

                </div>


                <div className="map-container">


                    <MapContainer

                        center={mapCenter}

                        zoom={5}

                        scrollWheelZoom={true}

                        style={{
                            height: "500px",
                            width: "100%"
                        }}

                    >


                        <TileLayer

                            attribution=
                                '&copy; OpenStreetMap contributors'

                            url=
                                "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"

                        />



                        {filteredMappedReports.map(
                            report => (

                                <Marker

                                    key={
                                        report.sosId
                                    }

                                    position={[
                                        Number(
                                            report.latitude
                                        ),

                                        Number(
                                            report.longitude
                                        )
                                    ]}

                                    icon={
                                        createPriorityIcon(
                                            report.priority
                                        )
                                    }

                                >


                                    <Popup>


                                        <div className="sos-popup">


                                            <h3>

                                                🚨
                                                {" "}
                                                {report.priority ||
                                                    "P3"}

                                                {" — "}

                                                {report.incidentType ||
                                                    "GENERAL"}

                                            </h3>


                                            <p>

                                                <strong>
                                                    Status:
                                                </strong>

                                                {" "}

                                                {getStatus(
                                                    report
                                                )}

                                            </p>


                                            <p>

                                                <strong>
                                                    Severity:
                                                </strong>

                                                {" "}

                                                {report.severity ||
                                                    "LOW"}

                                            </p>


                                            <p>

                                                <strong>
                                                    People:
                                                </strong>

                                                {" "}

                                                {report.peopleCount ||
                                                    "Unknown"}

                                            </p>


                                            <p>

                                                <strong>
                                                    Medical:
                                                </strong>

                                                {" "}

                                                {report.medicalNeeds
                                                    ? "REQUIRED"
                                                    : "No"}

                                            </p>


                                            <p>

                                                <strong>
                                                    Message:
                                                </strong>

                                                <br />

                                                {report.message ||
                                                    "No message"}

                                            </p>


                                            <p>

                                                <strong>
                                                    Resources:
                                                </strong>

                                                <br />

                                                {report
                                                    .requiredResources
                                                    ?.join(", ") ||
                                                    "None"}

                                            </p>


                                            <p>

                                                <strong>
                                                    Location:
                                                </strong>

                                                <br />

                                                {report.latitude},
                                                {" "}
                                                {report.longitude}

                                            </p>


                                            <p>

                                                <strong>
                                                    Received:
                                                </strong>

                                                <br />

                                                {formatTime(
                                                    report.receivedAt ||
                                                    report.timestamp
                                                )}

                                            </p>


                                        </div>


                                    </Popup>


                                </Marker>

                            )
                        )}


                    </MapContainer>


                </div>


            </section>


            {/* ==================================================
                FILTERS
            ================================================== */}

            <section className="filter-section">


                <input

                    type="text"

                    placeholder=
                        "🔎 Search emergency reports..."

                    value={search}

                    onChange={event =>
                        setSearch(
                            event.target.value
                        )
                    }

                    className="search-input"

                />


                <select

                    value={priorityFilter}

                    onChange={event =>
                        setPriorityFilter(
                            event.target.value
                        )
                    }

                    className="filter-select"

                >

                    <option value="ALL">
                        All Priorities
                    </option>

                    <option value="P0">
                        P0 Critical
                    </option>

                    <option value="P1">
                        P1 High
                    </option>

                    <option value="P2">
                        P2 Medium
                    </option>

                    <option value="P3">
                        P3 Low
                    </option>

                </select>


                <select

                    value={statusFilter}

                    onChange={event =>
                        setStatusFilter(
                            event.target.value
                        )
                    }

                    className="filter-select"

                >

                    <option value="ALL">
                        All Status
                    </option>

                    <option value="PENDING">
                        Pending
                    </option>

                    <option value="ACKNOWLEDGED">
                        Acknowledged
                    </option>

                    <option value="DISPATCHED">
                        Dispatched
                    </option>

                    <option value="RESOLVED">
                        Resolved
                    </option>

                </select>


                <button

                    className="refresh-button"

                    onClick={fetchReports}

                >

                    🔄 Refresh

                </button>


            </section>


            {/* ==================================================
                REPORTS
            ================================================== */}

            <section className="reports-section">


                <div className="section-header">


                    <div>

                        <h2>
                            🚨 Emergency Reports
                        </h2>

                        <p>
                            AI-prioritized incoming SOS reports
                        </p>

                    </div>


                    <span>

                        Showing{" "}
                        {filteredReports.length}
                        {" "}
                        of{" "}
                        {reports.length}

                    </span>


                </div>


                {loading ? (

                    <div className="empty">

                        Loading emergency reports...

                    </div>


                ) : filteredReports.length === 0 ? (

                    <div className="empty">

                        No matching SOS reports.

                    </div>


                ) : (


                    <div className="reports">


                        {filteredReports.map(
                            report => {


                                const currentStatus =
                                    getStatus(report);


                                return (

                                    <div

                                        className={
                                            `report-card ${
                                                getPriorityClass(
                                                    report.priority
                                                )
                                            }`
                                        }

                                        key={
                                            report.sosId
                                        }

                                    >


                                        {/* REPORT HEADER */}

                                        <div className="report-header">


                                            <div>

                                                <span

                                                    className={
                                                        `priority ${
                                                            getPriorityClass(
                                                                report.priority
                                                            )
                                                        }`
                                                    }

                                                >

                                                    {
                                                        report.priority ||
                                                        "P3"
                                                    }

                                                </span>


                                                <span className="incident">

                                                    {
                                                        report.incidentType ||
                                                        "GENERAL"
                                                    }

                                                </span>

                                            </div>


                                            <span className="severity">

                                                {
                                                    report.severity ||
                                                    "LOW"
                                                }

                                            </span>


                                        </div>


                                        {/* MESSAGE */}

                                        <h3>

                                            {
                                                report.message ||
                                                "No emergency message provided"
                                            }

                                        </h3>


                                        {/* DETAILS */}

                                        <div className="details">


                                            <div>

                                                👥

                                                <strong>
                                                    People:
                                                </strong>

                                                {" "}

                                                {
                                                    report.peopleCount ||
                                                    "Unknown"
                                                }

                                            </div>


                                            <div>

                                                🏥

                                                <strong>
                                                    Medical:
                                                </strong>

                                                {" "}

                                                {
                                                    report.medicalNeeds
                                                        ? "Required"
                                                        : "No"
                                                }

                                            </div>


                                            <div>

                                                📍

                                                <strong>
                                                    Location:
                                                </strong>

                                                {" "}

                                                {
                                                    report.latitude !==
                                                        null &&
                                                    report.latitude !==
                                                        undefined &&
                                                    report.longitude !==
                                                        null &&
                                                    report.longitude !==
                                                        undefined

                                                        ? `${report.latitude}, ${report.longitude}`

                                                        : "Unavailable"
                                                }

                                            </div>


                                            <div>

                                                🕒

                                                <strong>
                                                    Received:
                                                </strong>

                                                {" "}

                                                {
                                                    formatTime(
                                                        report.receivedAt ||
                                                        report.timestamp
                                                    )
                                                }

                                            </div>


                                        </div>


                                        {/* RESOURCES */}

                                        {report
                                            .requiredResources
                                            ?.length > 0 && (


                                            <div className="resources">


                                                <strong>

                                                    Required Resources

                                                </strong>


                                                <div className="resource-list">


                                                    {report
                                                        .requiredResources
                                                        .map(
                                                            resource => (

                                                                <span
                                                                    key={
                                                                        resource
                                                                    }
                                                                >

                                                                    {
                                                                        resource
                                                                    }

                                                                </span>

                                                            )
                                                        )}


                                                </div>


                                            </div>

                                        )}


                                        {/* STATUS CONTROL */}

                                        <div className="status-control">


                                            <strong>
                                                Rescue Status
                                            </strong>


                                            <select

                                                value={
                                                    currentStatus
                                                }

                                                onChange={event =>
                                                    updateStatus(
                                                        report.sosId,
                                                        event.target.value
                                                    )
                                                }

                                            >

                                                <option value="PENDING">
                                                    ⏳ Pending
                                                </option>

                                                <option value="ACKNOWLEDGED">
                                                    👁️Acknowledged
                                                </option>

                                                <option value="DISPATCHED">
                                                    🚑 Dispatched
                                                </option>

                                                <option value="RESOLVED">
                                                    ✅ Resolved
                                                </option>

                                            </select>


                                        </div>


                                        {/* FOOTER */}

                                        <div className="report-footer">


                                            <span>

                                                SOS ID:
                                                {" "}
                                                {report.sosId}

                                            </span>


                                            <span className="status">

                                                {currentStatus}

                                            </span>


                                        </div>


                                    </div>

                                );

                            }
                        )}


                    </div>

                )}


            </section>


        </div>

    );

}


export default App;