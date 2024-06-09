#include "CallWidget.h"
#include "ui_CallWidget.h"

#include <QDateTime>
#include <QJsonDocument>
#include <QJsonObject>
#include <QTimer>
#include <UserData.h>

CallWidget::CallWidget(QWidget *parent,
                       QString id,
                       QString groupId,
                       QString leaderId,
                       QString status,
                       QString title,
                       QString type,
                       qint64 scheduledTime)
    : QWidget(parent)
    , ui(new Ui::CallWidget)
{
    ui->setupUi(this);

    this->id = id;
    this->groupId = groupId;
    this->leaderId = leaderId;
    this->status = status;
    this->title = title;
    this->type = type;
    this->scheduledTime = scheduledTime;

    ui->labelCallTitle->setText(this->title);
    ui->label->hide();
    ui->labelPplCount->hide();

    if (this->type == "INSTANT")
        ui->labelScheduleDate->hide();

    if (this->type == "SCHEDULED") {
        qDebug() << this->scheduledTime << " " << QDateTime::currentSecsSinceEpoch();

        if (this->scheduledTime > (QDateTime::currentSecsSinceEpoch() + 3 * 3600))
            ui->joinButton->hide();

        ui->labelScheduleDate->setText(ui->labelScheduleDate->text() + " " + QDateTime::fromSecsSinceEpoch(this->scheduledTime - 3 * 3600).toString("yyyy-MM-dd hh:mm"));
        timer = new QTimer(this);

        QObject::connect(timer, &QTimer::timeout, this, [this]() {
            if (this->scheduledTime < (QDateTime::currentSecsSinceEpoch() + 3 * 3600)) {
                qDebug() << "hello";
                ui->joinButton->show();
                timer->stop();
            }
        });

        timer->start(60000);
    }
}

CallWidget::~CallWidget()
{
    delete ui;
}

QString CallWidget::getId()
{
    return this->id;
}

void CallWidget::on_joinButton_clicked()
{
    QJsonObject event;

    event.insert("EVENT", "JOIN_CALL");
    event.insert("groupId", this->groupId);
    event.insert("callId", this->id);
    event.insert("JWT", UserData::getInstance()->getJWT());

    QJsonDocument json(event);

    emit sendEvent(json);
}

