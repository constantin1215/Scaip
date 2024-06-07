#ifndef SCHEDULECALLDIALOG_H
#define SCHEDULECALLDIALOG_H

#include "UserData.h"
#include "ui_ScheduleCallDialog.h"
#include <QDialog>
#include <QJsonDocument>
#include <QJsonObject>

namespace Ui {
class ScheduleCallDialog;
}

class ScheduleCallDialog : public QDialog
{
    Q_OBJECT

public:
    explicit ScheduleCallDialog(QWidget *parent = nullptr, QString groupTitle = "", QString groupId = "");
    ~ScheduleCallDialog();
    QJsonDocument getEvent();

protected:
    void accept() override {
        if (!ui->inputCallTitle->text().isEmpty() && !ui->inputCallTitle->text().isNull() &&
            ui->dateTimeEdit->dateTime().toSecsSinceEpoch() > (QDateTime::currentSecsSinceEpoch() + 10)) {
            QJsonObject event;

            event.insert("EVENT", "NEW_CALL");
            event.insert("type", "SCHEDULED");
            event.insert("groupId", this->groupId);
            event.insert("title", ui->inputCallTitle->text());
            event.insert("date", ui->dateTimeEdit->dateTime().toString("dd/MM/yyyy hh:mm"));
            event.insert("JWT", UserData::getInstance()->getJWT());

            QJsonDocument json(event);

            this->event = json;

            QDialog::accept();
        }
        else
            ui->labelMessage->setText("Title must not be empty and date must be at least an hour in the future!");
    }

private slots:
    void on_buttonBox_accepted();

private:
    Ui::ScheduleCallDialog *ui;
    QJsonDocument event;
    QString groupId;
};

#endif // SCHEDULECALLDIALOG_H
