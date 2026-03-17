async function post() {
    const person = {
        id: document.getElementById("nr_id").value,
        lastName: document.getElementById("nume").value,
        firstName: document.getElementById("prenume").value,
        telephoneNumber: document.getElementById("nr_tel").value
    };

    const response = await fetch("/person", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(person)
    });
}