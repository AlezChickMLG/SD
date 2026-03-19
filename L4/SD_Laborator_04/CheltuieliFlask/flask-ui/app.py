from flask import Flask, render_template, request, session, redirect
from flask_session import Session
import requests

app = Flask(__name__)
SPRING_API = "https://localhost:8080"
app.secret_key = "secret_key"

@app.route("/", methods=["GET"])
def index():
    return render_template("index.html")

@app.route(f"/home", methods=["GET"])
def home():
    if "username" not in session:
        return redirect("/")

    username = session["username"]
    response = requests.get(f"{SPRING_API}/home/{username}", verify=False)

    if response.status_code != 200:
        return "Nu s-au putut incarca datele", 400

    data = response.json()

    return render_template("home.html", data=data)

@app.route("/register", methods=["POST"])
def register_post():
    username = request.form["user_register"]
    password = request.form["parola_register"]

    payload = {
        "username": username,
        "password": password,
        "salt": ""
    }

    response = requests.post(f"{SPRING_API}/register", json=payload, verify=False)

    if response.status_code == 201:
        return "Register complete"

    return "Register failed", 400

@app.route("/login", methods=["POST"])
def login_post():
    username = request.form["user_login"]
    password = request.form["parola_login"]

    payload = {
        "username": username,
        "password": password,
        "salt": "",
    }

    response = requests.post(f"{SPRING_API}/login", json=payload, verify=False)

    if response.status_code == 200:
        session["username"] = username
        return redirect(f"/home")

    return "Login failed", 400

if __name__ == "__main__":
    app.run(debug=True)