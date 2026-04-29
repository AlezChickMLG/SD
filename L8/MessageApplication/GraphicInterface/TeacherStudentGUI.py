from tkinter import *
from tkinter import ttk
import threading
import socket

HOST = "localhost"
TEACHER_PORT = 1600

STUDENT_HOST = ""
STUDENT_PORT = None

is_teacher_connected = False
is_connected = False

student_sock = None
teacher_sock = None

def resolve_question(question_text):
    # incercare de conectare catre microserviciul Teacher
    global teacher_sock
    global is_teacher_connected

    try:
        if not is_teacher_connected:
            teacher_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            teacher_sock.connect((HOST, TEACHER_PORT))
            is_teacher_connected = True

        teacher_sock.send(bytes(question_text + "\n", "utf-8"))

        # primire raspuns -> microserviciul Teacher foloseste coregrafia de microservicii pentru a trimite raspunsul inapoi
        response_text = str(teacher_sock.recv(1024), "utf-8")

    except ConnectionError:
        # in cazul unei erori de conexiune, se afiseaza un mesaj
        response_text = "Eroare de conectare la microserviciul Teacher!"

    # se adauga raspunsul primit in caseta text din interfata grafica
    response_widget.insert(END, response_text)

def resolve_student_question(port, question_text):
    global is_connected
    global student_sock
    global STUDENT_PORT

    try:
        #conectare la portul pentru gui al studentului
        if not is_connected or port != STUDENT_PORT:
            if student_sock is not None:
                student_sock.close()

            student_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            student_sock.connect((HOST, port))
            is_connected = True
            STUDENT_PORT = port

        #Trimitere mesaj de verificare
        student_sock.send(bytes(question_text + "\n", "utf-8"))
    except Exception as e:
        print("Eroare la conectare sau trimitere: " + str(e))

    response_widget.insert(END, "Am trimis un mesaj studentului")

def ask_question():
    # preluare text intrebare de pe interfata grafica
    question_text = question.get()

    # pornire thread separat pentru tratarea intrebarii respective
    # astfel, nu se blocheaza interfata grafica!
    threading.Thread(target=resolve_question, args=(question_text,)).start()

def ask_student_question():
    port = student_port.get()
    question_text = student_question.get()

    if question_text:
        try:
            port = int(port)
            resolve_student_question(port, question_text)

        except ValueError as e:
            response_widget.insert(END, "[Eroare]: Introdu un port valabil")

if __name__ == '__main__':
    connected = False

    root = Tk()
    root.title("Interactiune profesor-studenti")

    root.columnconfigure(0, weight=1)
    root.rowconfigure(0, weight=1)

    content = ttk.Frame(root, padding=10)
    content.grid(column=0, row=0)

    # ================= STÂNGA =================
    response_widget = Text(content, height=15, width=55)
    response_widget.grid(column=0, row=0, rowspan=8, padx=(0, 20))

    # ================= MIJLOC (Profesor) =================
    question_label = ttk.Label(content, text="Profesorul intreaba:")
    question_label.grid(column=1, row=0, columnspan=2, pady=(0, 10))

    question = ttk.Entry(content, width=35)
    question.grid(column=1, row=1, columnspan=2, pady=(0, 10))

    ask = ttk.Button(content, text="Intreaba", command=ask_question)
    ask.grid(column=1, row=2, pady=5)

    exitbtn = ttk.Button(content, text="Iesi", command=root.destroy)
    exitbtn.grid(column=2, row=2, pady=5)

    # ================= DREAPTA (Student) =================
    student_title = ttk.Label(
        content,
        text="Studentul intreaba:",
        font=("Arial", 10, "bold")
    )
    student_title.grid(column=3, row=0, columnspan=2, pady=(0, 15), padx=(30, 0))

    # Port
    port_label = ttk.Label(content, text="Port:")
    port_label.grid(column=3, row=1, sticky="w", padx=(30, 5))

    student_port = ttk.Entry(content, width=25)
    student_port.grid(column=4, row=1, pady=5)

    # Intrebare
    student_question_label = ttk.Label(content, text="Intrebare:")
    student_question_label.grid(column=3, row=2, sticky="w", padx=(30, 5))

    student_question = ttk.Entry(content, width=25)
    student_question.grid(column=4, row=2, pady=5)

    # Buton jos, centrat
    student_ask = ttk.Button(
        content,
        text="Intreaba",
        command=ask_student_question
    )
    student_ask.grid(column=3, row=4, columnspan=2, pady=15)

    root.mainloop()

