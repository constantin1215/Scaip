#ifndef NEWGROUPDIALOG_H
#define NEWGROUPDIALOG_H

#include "qjsonarray.h"
#include "qjsondocument.h"
#include "ui_NewGroupDialog.h"
#include <QDialog>
#include <QJsonObject>
#include <UserData.h>

namespace Ui {
class NewGroupDialog;
}

class NewGroupDialog : public QDialog
{
    Q_OBJECT

public:
    explicit NewGroupDialog(QWidget *parent = nullptr);
    ~NewGroupDialog();

    QString getTitle();
    QString getDescription();
    QJsonDocument getEvent();

public Q_SLOTS:
    void triggerPassToSearchDialog(QJsonObject eventData);

protected:
    void accept() override {
        if (!ui->input_title->text().isEmpty() && !ui->input_title->text().isNull() &&
            !ui->input_description->text().isEmpty() && !ui->input_description->text().isNull()) {
            ui->messageLabel->setText("");

            QJsonObject event;
            event.insert("EVENT", "CREATE_GROUP");
            event.insert("title", ui->input_title->text());
            event.insert("description", ui->input_description->text());
            event.insert("members", this->members);
            event.insert("JWT", UserData::getInstance()->getJWT());

            QJsonDocument json(event);

            this->event = json;

            QDialog::accept();
        }
        else
            ui->messageLabel->setText("Please set a title and description for the group!");
    }

private slots:
    void on_buttonBox_accepted();

    void on_addMemberButton_clicked();

Q_SIGNALS:
    void passToSearchDialog(QJsonObject eventData);

private:
    Ui::NewGroupDialog *ui;
    QString title;
    QString description;
    QJsonArray members;
    QJsonDocument event;
};

#endif // NEWGROUPDIALOG_H
