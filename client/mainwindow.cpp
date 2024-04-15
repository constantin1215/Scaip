#include "mainwindow.h"
#include "ui_mainwindow.h"

enum class Pages {
    MAIN_PAGE = 0,
    LOG_IN_PAGE = 1,
    REGISTER_PAGE = 2
};

enum class Events {
    LOG_IN,
    REGISTER
};

QString eventToString(Events event) {
    switch(event) {
        case Events::LOG_IN:
            return "LOG_IN";
        case Events::REGISTER:
            return "REGISTER";
    }
}

MainWindow::MainWindow(QWidget *parent, WSClient *client)
    : QMainWindow(parent)
    , ui(new Ui::MainWindow)
{
    ui->setupUi(this);
    ui->stackedWidget->setCurrentIndex(0);
    this->client = client;
}

MainWindow::~MainWindow()
{
    delete ui;
}

void MainWindow::on_loginButton_clicked()
{
    // QJsonObject loginEvent;

    // loginEvent.insert("EVENT", "LOG_IN");
    // loginEvent.insert("username", "costi1");
    // loginEvent.insert("password", "1234");

    // QJsonDocument json(loginEvent);

    // this->client->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));
    ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::LOG_IN_PAGE));
}


void MainWindow::on_registerButton_clicked()
{
    ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::REGISTER_PAGE));
}


void MainWindow::on_backButton_clicked()
{
    ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::MAIN_PAGE));
}


void MainWindow::on_backButton_2_clicked()
{
    ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::MAIN_PAGE));
}


void MainWindow::on_log_in_Button_clicked()
{
    QString username = ui->input_username_log_in->text();
    QString password = ui->input_password_log_in->text();

    QJsonObject event;
    event.insert("EVENT", eventToString(Events::LOG_IN));
    event.insert("username", username);
    event.insert("password", password);

    QJsonDocument json(event);

    this->client->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));
}


void MainWindow::on_register_Button_clicked()
{
    QString username = ui->input_username_register->text();
    QString password = ui->input_password_register->text();
    QString email = ui->input_email_register->text();
    QString firstName = ui->input_first_name_register->text();
    QString lastName = ui->input_last_name_register->text();

    QJsonObject event;

    event.insert("EVENT", eventToString(Events::REGISTER));
    event.insert("username", username);
    event.insert("password", password);
    event.insert("email", email);
    event.insert("firstName", firstName);
    event.insert("lastName", lastName);

    QJsonDocument json(event);

    this->client->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));
}

