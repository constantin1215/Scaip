#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <WSClient.h>
#include <QJsonObject>
#include <QJsonDocument>

QT_BEGIN_NAMESPACE
namespace Ui {
class MainWindow;
}
QT_END_NAMESPACE

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    MainWindow(QWidget *parent = nullptr, WSClient *client = nullptr);
    ~MainWindow();

private slots:
    void on_loginButton_clicked();

    void on_registerButton_clicked();

    void on_backButton_clicked();

    void on_backButton_2_clicked();

    void on_log_in_Button_clicked();

    void on_register_Button_clicked();

private:
    Ui::MainWindow *ui;
    WSClient *client;
};
#endif // MAINWINDOW_H
