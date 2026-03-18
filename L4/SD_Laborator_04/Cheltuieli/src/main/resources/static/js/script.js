async function login() {
    let user = document.getElementById("user_login").value;
    let password = document.getElementById("parola_login").value;

    let responseBody = {
        username: user,
        password: password
    };

    let response = await fetch("/login", {
        method: "POST",
        headers: {
             "Content-Type": "application/json"
        },
        body: JSON.stringify(responseBody)
    });

    if (response.ok) {
        window.location.href = "/home";
    }

    else {
        alert("Username sau parola gresita");
    }
}

async function register() {
    let user = document.getElementById("user_register").value;
    let password = document.getElementById("parola_register").value;

    let responseBody = {
        username: user,
        password: password
    };

    let response = await fetch("/register", {
        method: "POST",
        headers: {
             "Content-Type": "application/json"
        },
        body: JSON.stringify(responseBody)
    });
}