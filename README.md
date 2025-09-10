# TrustBubble 🛡️

**TrustBubble** is an intelligent Android application that helps you verify on-screen content in real time.  
It uses a floating bubble overlay to analyze screenshots, news headlines, or text for **safety** and **reliability** using the **Google Gemini AI**.

---

## 📌 What Does It Do? (Simple Explanation)

Think of TrustBubble as a helpful assistant that's always available on your screen:

1. **See Something Interesting?** → While browsing social media or a news site, tap the floating bubble.  
2. **Instant Analysis** → The app takes a quick screenshot and sends it to our smart backend server.  
3. **Get a Simple Answer** → The server asks the Gemini AI: *"Is this content safe and reliable?"*  
   - ✅ Good → Safe & trustworthy  
   - ⚠️ Harmful → Unreliable or unsafe  

---

## ✨ Key Features

- 🟢 **Floating Bubble Interface** → Always on top, draggable, and easy to use.  
- 📸 **One-Tap Screen Analysis** → Capture your screen instantly for fact-checking.  
- 🤖 **AI-Powered Fact-Checking** using **Google Gemini API**:
  - **Safety** → Detects harmful or sensitive material.  
  - **Reliability** → Evaluates news headlines & claims for trustworthiness.  
- 🧩 **Explainable AI (XAI)** → See the AI’s step-by-step reasoning in a decision tree format.  
- 📂 **Analysis History** → Review past checks anytime.  
- 🎨 **Visual Feedback** → Bubble pulses during analysis, fades during cooldown.  

---

## 🛠️ Tech Stack

### Frontend (Android App)
- **Language**: Kotlin  
- **UI**: Material 3, View Binding, Navigation Component  
- **Networking**: Retrofit  

### Backend (Server)
- **Framework**: Python (FastAPI)  
- **Database**: SQLite  

### AI
- **Model**: Google Gemini 1.5 Pro  

---

## ⚙️ How It Works (Client-Server Architecture)

- **Android App (Client)** → Like a **photographer**. Captures screenshots and sends them to the server.  
- **Python Server (Backend)** → Like a **photo lab**. Processes the screenshot, sends it to Gemini AI, and returns results.  

---

## 🚀 Setup and Installation

### ✅ Prerequisites
- [Android Studio](https://developer.android.com/studio) (latest version)  
- [Python 3.8+](https://www.python.org/downloads/)  
- A **Google Gemini API Key**  

---

### 1️⃣ Backend Setup

```bash
# Navigate to the backend folder
cd Trust_Bubble_Backend

# Create a virtual environment
# Windows
python -m venv .venv
.\.venv\Scripts\Activate.ps1

# macOS/Linux
python3 -m venv .venv
source .venv/bin/activate

# Install dependencies
pip install -r requirements.txt

```

### 🔑 Add Your API Key

Create a `.env` file in `Trust_Bubble_Backend` and add:

```env
GEMINI_API_KEY="your_secret_api_key_goes_here"
DATABASE_URL="sqlite:///./trustbubble.db"
```

### 🔑 Run the backedn
uvicorn main:app --host 0.0.0.0 --port 8000
---
### 2️⃣ Frontend Setup (Android App)

Open the project in Android Studio.

Update the server address in:

```
app/src/main/java/com/example/trust_bubble/network/RetrofitClient.kt
```

##If phone & PC are on the same Wi-Fi → use your PC’s private IP:

``` 
private const val BASE_URL = "http://192.168.1.10:8000/"
```


## If on different networks → use ngrok:

``` 
private const val BASE_URL = "https://your-ngrok-url.ngrok-free.app/"
```


## Build and run the app on an Android device or emulator.

### 📜 License

Licensed under Google’s API usage policies.
This project is for educational and research purposes.

### 🤝 Contributing

Contributions are welcome! Please open an issue or pull request for suggestions, bug fixes, or improvements.

### ⭐ Support

If you find this project helpful, consider giving it a star ⭐ on GitHub!
