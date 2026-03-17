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

async function getAgenda() {
    const persons = await fetch("/agenda");
    const data = await persons.json();

    let personString = "";

    for (let i = 0; i < data.length; i++) {
        let id = data[i].id;
        let lastName = data[i].lastName;
        let firstName = data[i].firstName;
        let telephoneNumber = data[i].telephoneNumber;

        let person = "ID: " + id + "\nlastName: " + lastName + "\nFirstName: " + firstName + "\nTel. number: " + telephoneNumber + "\n\n";
        personString += person;
    }

    document.getElementById("agendaShow").innerText = personString;
}

async function put() {
    let nr_id = document.getElementById("nr_id").value;
    let url = "/person/" + nr_id;

    let person = {
        id: nr_id,
        lastName: document.getElementById("nume").value,
        firstName: document.getElementById("prenume").value,
        telephoneNumber: document.getElementById("nr_tel").value
    };

    const response = await fetch(url, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(person)
    });
}

async function deletePerson() {
    let nr_id = document.getElementById("nr_id").value;
    let url = "/person/" + nr_id;

    let response = await fetch(url, {
            method: "DELETE"
    });
}