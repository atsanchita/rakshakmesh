# 🚨 RakshakMesh

### An Offline-First Emergency Communication and Response Network for Disaster Resilience 

> **"When the network fails, the lifeline stays connected throught RakshakMesh"**

RakshakMesh is an **offline-first emergency communication and response system** designed for disaster situations where conventional internet and cellular connectivity may become unavailable, unreliable, or congested.

The system allows citizens to create emergency SOS reports directly from their smartphones, store those reports locally, and opportunistically transfer them to nearby devices using **peer-to-peer communication**. Once an available gateway gains internet connectivity, the accumulated emergency reports can be synchronized with a backend server.

An **AI-assisted prioritization layer** helps classify incoming emergencies based on factors such as the number of affected people, medical conditions, fire, flooding, structural damage, and other emergency indicators.

A web-based **Rescue Command Dashboard** allows responders to visualize emergency reports on a map, inspect their details, filter incidents, and track response status.

---

## 📌 Table of Contents

- [Problem](#-problem)
- [Our Solution](#-our-solution)
- [Core Idea](#-core-idea)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [End-to-End Data Flow](#-end-to-end-data-flow)
- [How the Offline Communication Works](#-how-the-offline-communication-works)
- [AI-Assisted Prioritization](#-ai-assisted-prioritization)
- [Rescue Dashboard](#-rescue-dashboard)
- [SOS Data Model](#-sos-data-model)
- [Technology Stack](#-technology-stack)
- [Repository Structure](#-repository-structure)
- [Getting Started](#-getting-started)
- [Android Application](#-android-application)
- [Backend](#-backend)
- [Rescue Dashboard Setup](#-rescue-dashboard-setup)
- [API Endpoints](#-api-endpoints)
- [Demo Flow](#-demo-flow)
- [Engineering Decisions](#-engineering-decisions)
- [Current MVP Scope](#-current-mvp-scope)
- [Limitations](#-limitations)
- [Future Evolution](#-future-evolution)
- [Deployment Architecture](#-deployment-architecture)
- [Why RakshakMesh](#-why-rakshakmesh)
- [Team](#-team)

---

# 🌪️ Problem

During disasters such as:

- Floods
- Earthquakes
- Cyclones
- Landslides
- Fires
- Building collapses
- Major infrastructure failures

communication infrastructure can become unreliable.

Citizens may be unable to reach emergency services because:

- Cellular towers may be damaged.
- Internet connectivity may be unavailable.
- Networks may become congested.
- Power infrastructure may fail.
- Emergency information may remain trapped with individual citizens.
- Rescue teams may not have a unified view of incidents.
- Information arriving from different sources may lack consistent priority or structure.

The result is a critical communication gap between:

**Citizen → Nearby Community → Rescue Gateway → Emergency Response Team**

RakshakMesh is designed to address this gap.

---

# 💡 Our Solution

RakshakMesh creates an **offline-first emergency communication pipeline**.

Instead of requiring every citizen to have direct internet access to an emergency server:

```text
Citizen
   ↓
Creates SOS locally
   ↓
Nearby device
   ↓
Peer-to-peer relay
   ↓
Another device / responder / gateway
   ↓
Internet-connected gateway
   ↓
Backend
   ↓
AI-assisted prioritization
   ↓
Rescue Command Dashboard

The fundamental principle is:

Emergency information should not disappear simply because the internet is unavailable at the moment it is created.

🔗 Core Idea

RakshakMesh combines five major concepts:

1. Offline-first SOS creation

A citizen can create an emergency report without requiring an active internet connection.

The report is stored locally on the device.

2. Peer-to-peer communication

Nearby devices can communicate using Google's Nearby Connections framework.

This allows emergency information to move between nearby devices without requiring the normal internet path.

3. Store-and-forward

A device can receive an SOS, store it locally, and forward it when another suitable peer becomes available.

This creates an opportunistic communication path.

4. Gateway synchronization

When a connected gateway or responder device becomes available, stored emergency reports can be synchronized with the backend.

5. AI-assisted response prioritization

The backend analyzes incoming SOS information and assigns:

Incident type
Severity
Priority
Medical requirement
Required resources

This helps responders organize incoming emergencies.

🚨 Key Features
Feature	Description
📱 Offline SOS	Create emergency reports without requiring internet
💾 Local Persistence	Emergency reports remain stored on the device
📡 P2P Communication	Nearby devices exchange SOS information
🔄 Store-and-Forward	Reports can move opportunistically across devices
📍 GPS Location	SOS reports include geographic coordinates
☁️ Gateway Sync	Connected gateways upload reports to the backend
🤖 AI Prioritization	Incoming reports receive structured priority information
🗺️ Live Map	Rescue teams can view incidents geographically
🔎 Search & Filters	Responders can filter emergency reports
🚨 Priority Visualization	P0/P1/P2/P3 incidents are visually differentiated
📋 Incident Details	Responders can inspect complete SOS information
🔄 Response Tracking	Incidents can move through response states
🏗️ System Architecture
                  ┌───────────────────────┐
                  │     CITIZEN DEVICE    │
                  │                       │
                  │  Android Application  │
                  │  • SOS Creation       │
                  │  • GPS Location       │
                  │  • Local Storage      │
                  └───────────┬───────────┘
                              │
                              │ Nearby P2P
                              ▼
                  ┌───────────────────────┐
                  │    RELAY DEVICES      │
                  │                       │
                  │  • Receive SOS        │
                  │  • Store SOS          │
                  │  • Forward SOS        │
                  └───────────┬───────────┘
                              │
                              │ Opportunistic
                              │ Communication
                              ▼
                  ┌───────────────────────┐
                  │    RESCUE GATEWAY     │
                  │                       │
                  │  • Collect Reports    │
                  │  • Internet Available │
                  │  • Sync to Backend    │
                  └───────────┬───────────┘
                              │
                              │ HTTPS / REST
                              ▼
                  ┌───────────────────────┐
                  │       BACKEND         │
                  │                       │
                  │  Node.js + Express    │
                  │  MongoDB + Mongoose   │
                  │  AI Prioritization    │
                  └───────────┬───────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │   RESCUE DASHBOARD    │
                  │                       │
                  │ React + Vite          │
                  │ Leaflet Map           │
                  │ Incident Management   │
                  └───────────────────────┘
🔄 End-to-End Data Flow

A complete RakshakMesh emergency flow is:

PHONE A
  │
  │ 1. Citizen creates SOS
  ▼
LOCAL STORAGE
  │
  │ 2. SOS remains available offline
  ▼
PHONE B
  │
  │ 3. Nearby P2P transfer
  ▼
PHONE C / GATEWAY
  │
  │ 4. Store-and-forward
  ▼
BACKEND API
  │
  │ 5. REST API synchronization
  ▼
MONGODB
  │
  │ 6. Persistent incident record
  ▼
AI SERVICE
  │
  │ 7. Priority + classification
  ▼
RESCUE DASHBOARD
  │
  │ 8. Map + incident details
  ▼
RESPONDER
  │
  ├── ACKNOWLEDGED
  ├── DISPATCHED
  └── RESOLVED
📡 How the Offline Communication Works

RakshakMesh uses peer-to-peer communication to transfer emergency information between nearby devices.

The Android application uses:

Google Nearby Connections

with:

Strategy: P2P_CLUSTER

A device can:

Advertise itself.
Discover nearby RakshakMesh devices.
Establish a connection.
Exchange emergency information.
Store received reports locally.
Forward reports to other available peers.
Store-and-forward principle

Suppose:

Citizen A
   ↓
Phone B
   ↓
Phone C
   ↓
Gateway

Citizen A does not need direct internet connectivity.

The SOS can move through available communication opportunities until it reaches a gateway.

Important design principle

RakshakMesh is offline-first, but offline does not mean communication happens without any physical communication medium.

Nearby devices still need an available mechanism such as Bluetooth/Wi-Fi-based peer connectivity.

The MVP demonstrates opportunistic P2P relay, rather than claiming guaranteed nationwide mesh coverage.

💾 Local Persistence

SOS reports are stored locally on the Android device.

The MVP uses local persistence to ensure that an emergency report does not depend on immediate backend connectivity.

A locally created SOS contains information such as:

SOS ID
Message
People Count
Condition
Latitude
Longitude
Timestamp
Status

Received SOS reports can also be persisted locally.

This enables the store-and-forward model.

📍 Location Intelligence

RakshakMesh captures the current device location using the Android location services.

The MVP uses:

Fused Location Provider

to obtain:

Latitude
Longitude

The coordinates are transferred with the SOS report and stored in MongoDB.

The rescue dashboard then visualizes these coordinates on a map.

If precise automatic location cannot be obtained, a future version can support manual location entry or other fallback mechanisms.

🤖 AI-Assisted Prioritization

RakshakMesh includes an AI-assisted prioritization layer on the backend.

The purpose is not to replace rescue authorities.

Instead, it helps transform unstructured citizen reports into more organized emergency information.

The system considers signals such as:

Number of affected people
Medical emergencies
Fire
Flooding
Earthquake
Landslide
Rescue requirements
Structural danger
Other critical keywords

The service produces structured information such as:

Incident Type
Severity
Priority
Medical Requirement
Required Resources
🚨 Priority Levels

RakshakMesh uses four priority levels:

Priority	Meaning
🔴 P0	Critical / potentially life-threatening
🟠 P1	High urgency
🟡 P2	Medium urgency
🟢 P3	Lower urgency / informational

Examples of information that may influence priority include:

"10 people trapped and injured"
        ↓
Higher priority

"Fire and breathing difficulty"
        ↓
Higher priority

"Road blocked"
        ↓
Medium priority

The current MVP uses a deterministic rules-based AI service so that the behavior is:

Fast
Explainable
Reproducible
Easy to demonstrate

Future versions can incorporate more advanced machine-learning or language-model-based classification.

🗺️ Rescue Command Dashboard

The Rescue Dashboard provides responders with a centralized view of incoming emergency reports.

Dashboard capabilities
Live incident retrieval

The dashboard periodically retrieves SOS reports from the backend.

Interactive map

Emergency reports are displayed geographically using:

Leaflet + OpenStreetMap

Priority markers

Markers visually represent:

P0
P1
P2
P3
Search

Responders can search emergency reports using report information.

Filtering

Reports can be filtered by:

Priority
Status
Incident details

Each incident can display:

SOS ID
Message
People count
Condition
Location
Timestamp
Incident type
Severity
Medical requirements
Required resources
Priority
Response workflow

The MVP supports:

PENDING
    ↓
ACKNOWLEDGED
    ↓
DISPATCHED
    ↓
RESOLVED
🧾 SOS Data Model

A typical emergency report contains:

{
  "sosId": "unique-id",
  "message": "Building collapsed and people are trapped",
  "peopleCount": "5",
  "condition": "Injured",
  "latitude": 19.0760,
  "longitude": 72.8777,
  "timestamp": "2026-09-16T12:00:00Z",
  "status": "PENDING"
}

After backend processing, additional information can include:

priority
severity
incidentType
medicalNeeds
requiredResources
receivedAt
🛠️ Technology Stack
📱 Citizen Application
Technology	Purpose
Kotlin	Android development
Jetpack Compose	UI
Android SDK	Mobile platform
Google Nearby Connections	Peer-to-peer communication
Fused Location Provider	GPS/location
SharedPreferences	MVP local persistence

Minimum Android SDK: API 26

☁️ Backend
Technology	Purpose
Node.js	Runtime
Express.js	REST API
Mongoose	MongoDB ODM
MongoDB Atlas	Database
dotenv	Environment configuration
JavaScript	Backend implementation
🤖 AI Layer

The current MVP uses a deterministic rules-based prioritization service.

It analyzes:

People count
Emergency keywords
Incident type
Medical indicators
Rescue indicators
Severity indicators

and generates structured priority information.

🖥️ Rescue Dashboard
Technology	Purpose
React	UI
Vite	Development/build tooling
Leaflet	Interactive map
OpenStreetMap	Map tiles
JavaScript	Frontend implementation
📂 Repository Structure
RakshakMesh/
│
├── app/                         # Android application
│
├── backend/                     # Node.js backend
│   ├── src/
│   │   ├── config/
│   │   │   └── db.js
│   │   │
│   │   ├── controllers/
│   │   │   └── sos.controller.js
│   │   │
│   │   ├── middleware/
│   │   │   └── error.middleware.js
│   │   │
│   │   ├── models/
│   │   │   └── sos.model.js
│   │   │
│   │   ├── routes/
│   │   │   └── sos.routes.js
│   │   │
│   │   ├── services/
│   │   │   ├── ai.service.js
│   │   │   └── sos.service.js
│   │   │
│   │   ├── app.js
│   │   └── server.js
│   │
│   ├── package.json
│   └── .gitignore
│
├── dashboard/                   # React rescue dashboard
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── vite.config.js
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── .gitignore
└── README.md
🚀 Getting Started

RakshakMesh consists of three major components:

1. Android Application
2. Backend API
3. Rescue Dashboard

Run each component separately during development.

📱 Android Application
Requirements
Android Studio
Android SDK
Android device/emulator
Kotlin support
Run

Open the root RakshakMesh project in Android Studio.

Sync Gradle and run the application on an Android device.

For real P2P testing, physical Android devices are recommended.

Important

The Android application communicates with the backend through the configured API URL.

For local testing, the backend device/computer and Android phone should be reachable over the same local network.

Update the backend URL in the Android application when necessary.

Example:

private val backendUrl =
    "http://YOUR_LOCAL_IP:5000/api/v1/sos"

Do not commit private or environment-specific credentials.

☁️ Backend

Navigate to the backend:

cd backend

Install dependencies:

npm install

Create a .env file:

PORT=5000
MONGO_URI=YOUR_MONGODB_CONNECTION_STRING

Start the development server:

npm run dev

Or:

npm start

The backend runs on:

http://localhost:5000
🗄️ MongoDB

RakshakMesh uses MongoDB Atlas for persistent backend storage.

The backend connects using:

MONGO_URI=YOUR_MONGODB_CONNECTION_STRING

The .env file is intentionally excluded from Git.

Never commit:

MongoDB passwords
API keys
Cloud credentials
Private secrets
🖥️ Rescue Dashboard Setup

Navigate to:

cd dashboard

Install dependencies:

npm install

Start the development server:

npm run dev

Vite will provide the local dashboard URL, typically:

http://localhost:5173

The dashboard communicates with the backend API.

For local development:

Dashboard
    ↓
http://localhost:5000
    ↓
Backend
    ↓
MongoDB
🔌 API Endpoints
Create / Synchronize SOS
POST /api/v1/sos

Example:

{
  "sosId": "abc-123",
  "message": "People trapped after building collapse",
  "peopleCount": "5",
  "condition": "Injured",
  "latitude": 19.0760,
  "longitude": 72.8777,
  "timestamp": "2026-09-16T12:00:00Z",
  "status": "PENDING"
}

The backend:

Receives the SOS.
Checks the SOS identifier.
Prevents duplicate insertion.
Runs AI-assisted prioritization.
Stores the processed report in MongoDB.
Retrieve SOS Reports
GET /api/v1/sos

This endpoint provides emergency reports for the rescue dashboard.

🧪 Demo Flow

The complete MVP demonstration follows:

STEP 1
Citizen opens RakshakMesh
        ↓

STEP 2
Citizen creates an SOS
        ↓

STEP 3
SOS is stored locally
        ↓

STEP 4
Nearby device discovers the sender
        ↓

STEP 5
SOS is transferred through P2P
        ↓

STEP 6
Receiving device stores the SOS
        ↓

STEP 7
SOS is forwarded toward an available gateway
        ↓

STEP 8
Gateway synchronizes with backend
        ↓

STEP 9
Backend stores the report in MongoDB
        ↓

STEP 10
AI-assisted service determines priority
        ↓

STEP 11
Rescue Dashboard receives the incident
        ↓

STEP 12
Incident appears on the map
        ↓

STEP 13
Responder acknowledges the incident
        ↓

STEP 14
Responder dispatches assistance
        ↓

STEP 15
Incident is marked resolved
🎯 MVP Proof

The MVP is designed to demonstrate the complete chain:

Offline SOS
     +
Local Persistence
     +
P2P Communication
     +
Store-and-Forward
     +
Gateway Synchronization
     +
Backend
     +
MongoDB
     +
AI Prioritization
     +
Rescue Dashboard
     =
End-to-End Disaster Response Proof of Concept
🧠 Engineering Decisions
Why Offline-First?

Disaster communication cannot assume that internet connectivity will always exist.

The first objective is therefore:

Capture and preserve emergency information first. Synchronize when connectivity becomes available.

Why Peer-to-Peer?

A citizen may not have access to the internet but may still be physically close to another device.

P2P communication provides an additional communication path between nearby devices.

Why Store-and-Forward?

A disaster environment is dynamic.

A gateway may not be immediately reachable.

Instead of discarding an SOS:

Receive
   ↓
Store
   ↓
Wait for opportunity
   ↓
Forward

This makes emergency information more resilient to intermittent connectivity.

Why MongoDB?

SOS reports contain flexible emergency information and may evolve over time.

MongoDB provides a convenient document-oriented structure for the MVP.

Why Rules-Based AI for the MVP?

A hackathon prototype benefits from AI behavior that is:

Explainable
Fast
Deterministic
Easy to test
Easy to demonstrate

The current system therefore uses a structured rules-based prioritization layer.

More advanced ML/LLM approaches can be introduced later.

⚠️ Important Architecture Clarification

RakshakMesh does not claim that smartphones can provide unlimited or guaranteed communication coverage during a disaster.

The MVP demonstrates:

Opportunistic peer-to-peer emergency information propagation across available nearby devices.

A production deployment would require a layered communication infrastructure.

For example:

Citizen Smartphones
        ↓
Responder Smartphones
        ↓
Portable Emergency Gateways
        ↓
Fixed Emergency Infrastructure
        ↓
Long-Range / Satellite / Cellular Backhaul
        ↓
Central Emergency Platform

This layered approach is intended to increase resilience beyond relying on a single communication mechanism.

📊 Current MVP Scope
Implemented
 Android SOS creation
 Offline-first SOS creation
 Local SOS persistence
 GPS acquisition
 Nearby Connections
 Device discovery
 P2P SOS transfer
 Local reception/storage
 Gateway synchronization
 Node.js backend
 MongoDB Atlas
 SOS deduplication
 AI-assisted prioritization
 Incident classification
 Priority assignment
 React rescue dashboard
 Interactive map
 SOS markers
 Search
 Filters
 Priority visualization
 Response status workflow
 End-to-end demo flow
⚠️ Current Limitations

RakshakMesh is a proof-of-concept MVP and not yet a production emergency-response infrastructure.

Current limitations include:

1. P2P range

Communication depends on nearby devices and available radio connectivity.

2. Device availability

Store-and-forward works only when suitable relay devices become available.

3. Gateway dependency for backend synchronization

The central backend requires a gateway or other connected node to upload reports.

4. Dashboard response persistence

The current MVP response workflow is primarily implemented at the dashboard layer and can be further integrated with backend persistence.

5. Security hardening

A production deployment would require stronger:

Authentication
Authorization
Encryption
Device identity
Message integrity
Abuse prevention
Audit logging
6. Large-scale mesh management

Production deployment would require advanced:

Routing
TTL management
Message expiry
Relay tracking
Network discovery
Duplicate suppression
Fault tolerance
🔮 Future Evolution

RakshakMesh can evolve into a larger emergency communication platform.

Phase 1 — MVP
Android
+
P2P
+
Local Storage
+
Gateway
+
Backend
+
AI
+
Dashboard
Phase 2 — Resilient Mesh

Introduce:

Multi-hop routing
Relay history
TTL
Message hashing
Strong deduplication
Delivery acknowledgements
Better retry mechanisms
Phase 3 — Intelligent Emergency Network

Introduce advanced AI capabilities:

NLP-based incident extraction
Multilingual emergency reports
Automatic incident classification
Resource recommendation
Duplicate incident detection
Emergency clustering
Situation summarization
Phase 4 — Advanced Geospatial Intelligence

Potential capabilities:

Disaster heatmaps
Incident clustering
Safe-zone mapping
Road accessibility
Rescue route planning
Infrastructure damage mapping
Geographic risk analysis
Phase 5 — Multi-Layer Communication

A production-grade system could integrate:

Bluetooth
Wi-Fi Direct / Nearby P2P
Portable gateways
Cellular
Long-range radio
Satellite connectivity
Emergency communication infrastructure

The objective would be to provide multiple communication paths rather than depending on a single network.

☁️ Deployment Architecture

The planned cloud architecture is:

                    INTERNET
                       │
          ┌────────────┴────────────┐
          │                         │
          ▼                         ▼
   React Dashboard             Android App
      Vercel                  HTTPS API
          │                         │
          └───────────┬─────────────┘
                      ▼
              Node.js Backend
                  Render
                      │
                      ▼
               MongoDB Atlas

The Android application can communicate directly with the deployed backend through HTTPS.

The dashboard can also communicate with the same backend.

🔐 Security Considerations

A production version of RakshakMesh should implement:

End-to-end message encryption
Device authentication
Gateway authentication
API authentication
Role-based access control
Secure token management
Message integrity verification
Replay attack protection
Rate limiting
Audit logs
Secure location handling
Data retention policies
Privacy-aware emergency data management

Emergency location information should be treated as sensitive operational data.

🧪 Testing Strategy

RakshakMesh should be tested at multiple levels.

Unit Testing

Test:

SOS creation
Priority classification
People-count extraction
Incident classification
Data validation
Integration Testing

Test:

Android
   ↓
Backend
   ↓
MongoDB
P2P Testing

Test:

Phone A → Phone B

and progressively:

Phone A → Phone B → Phone C
End-to-End Testing

Validate:

SOS
→ P2P
→ Relay
→ Gateway
→ Backend
→ MongoDB
→ AI
→ Dashboard
🏆 Hackathon Demonstration Strategy

The strongest demonstration focuses on the actual problem:

Scenario

A disaster occurs.

Traditional internet connectivity is unavailable.

A citizen creates an SOS.

📱 Citizen
"I am trapped. 5 people injured."

The report is saved locally.

A nearby device receives it.

The report moves through the opportunistic network.

Eventually, a gateway reaches the backend.

The backend processes the report.

AI-assisted prioritization identifies the emergency.

The rescue dashboard displays:

🔴 P0 / High urgency
📍 Location
👥 People affected
🏥 Medical requirement
🚒 Required resources

The responder then changes:

PENDING
   ↓
ACKNOWLEDGED
   ↓
DISPATCHED
   ↓
RESOLVED

This demonstrates the complete journey of an emergency report.

💭 Why RakshakMesh?

Most emergency applications assume that the user can communicate with a centralized server.

RakshakMesh changes that assumption.

Instead of:

Citizen
   ↓
Internet
   ↓
Server

the system introduces:

Citizen
   ↓
Nearby Device
   ↓
Relay
   ↓
Gateway
   ↓
Server

This creates an additional path for emergency information when traditional connectivity is disrupted.

The central idea is simple:

Connectivity should be treated as an opportunity, not a prerequisite for capturing emergency information.

Project: RakshakMesh

Tagline:

When the network fails, the lifeline stays connected.

🌐 Project Vision

RakshakMesh is built around a simple long-term vision:

No emergency message should be lost merely because conventional connectivity failed at the moment it was needed most.

By combining:

Offline-First Design
        +
Peer-to-Peer Communication
        +
Store-and-Forward Networking
        +
Gateway Synchronization
        +
AI-Assisted Prioritization
        +
Geospatial Visualization

RakshakMesh aims to create a more resilient emergency communication and response workflow.

🚨 RakshakMesh
When the network fails, the lifeline stays connected.


