import os
import sys
import time
from abc import update_abstractmethods
from threading import Thread

import requests
import qdarkstyle
from requests.exceptions import HTTPError
from PyQt5.QtWidgets import (QWidget, QApplication, QFileDialog,
QMessageBox)
from PyQt5 import QtCore
from PyQt5.uic import loadUi
from rabbitMQ import RabbitMQ

def debug_trace(ui=None):
    from pdb import set_trace
    QtCore.pyqtRemoveInputHook()
    set_trace()
    # QtCore.pyqtRestoreInputHook()

class LibraryApp(QWidget):
    ROOT_DIR = os.path.dirname(os.path.abspath(__file__))

    def __init__(self):
        super(LibraryApp, self).__init__()
        ui_path = os.path.join(self.ROOT_DIR, 'library_manager.ui')
        loadUi(ui_path, self)
        self.search_btn.clicked.connect(self.search)
        self.save_as_file_btn.clicked.connect(self.save_as_file)

        self.rabbitMQ = RabbitMQ()
        self.last_result = None

        Thread(target=self.rabbitMQ.receive_message, daemon=True).start()

        self.timer = QtCore.QTimer(self)
        self.timer.timeout.connect(self.update_text)
        self.timer.start(100)

    def search(self):
        search_string = self.search_bar.text()
        command = None

        if not search_string:
            command = "getAllBooks"
            if self.json_rb.isChecked():
                command += "~json"

            elif self.html_rb.isChecked():
                command += "~html"

            else:
                command += "~raw"
        else:
            command = "findBook"
            pass
            if self.author_rb.isChecked():
                command += f"~author={search_string}"

            elif self.title_rb.isChecked():
                command += f"~title={search_string}"

            else:
                command += f"~publisher={search_string}"

        try:
            self.rabbitMQ.send_message(command)

        except Exception as err:
            print('Other error occurred: {}'.format(err))

    def update_text(self):
        current_result = self.rabbitMQ.result

        if current_result is not None and current_result != self.last_result:
            self.result.setText(current_result)
            self.last_result = current_result

    def save_as_file(self):
        options = QFileDialog.Options()
        options |= QFileDialog.DontUseNativeDialog
        file_path = str(QFileDialog.getSaveFileName(self, 'Salvare fisier',options=options))

        if file_path:
            file_path = file_path.split("'")[1]

            if not file_path.endswith('.json') and not file_path.endswith('.html') and not file_path.endswith('.txt'):
                if self.json_rb.isChecked():
                    file_path += '.json'

                elif self.html_rb.isChecked():
                    file_path += '.html'

                else:
                    file_path += '.txt'

            try:
                with open(file_path, 'w') as fp:
                    if file_path.endswith(".html"):
                        fp.write(self.result.toHtml())

                    else:
                        fp.write(self.result.toPlainText())

            except Exception as e:
                print(e)

            QMessageBox.warning(self, 'Library Manager', 'Nu s-a putut salva fisierul')

if __name__ == '__main__':
    app = QApplication(sys.argv)

    stylesheet = qdarkstyle.load_stylesheet_pyqt5()
    app.setStyleSheet(stylesheet)

    window = LibraryApp()
    window.show()
    sys.exit(app.exec_())