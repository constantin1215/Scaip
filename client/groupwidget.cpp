#include "groupwidget.h"
#include "ui_groupwidget.h"

#include <QDateTime>



GroupWidget::GroupWidget(QWidget *parent, QString id, QString groupName, QString lastMessage, qint64 timestamp, QString ownerId)
    : QWidget(parent)
    , ui(new Ui::GroupWidget)
{
    ui->setupUi(this);

    this->id = id;
    this->groupName = groupName;
    this->timestamp = timestamp;
    this->wasOpened = false;
    this->ownerId = ownerId;
    this->lastMessage = lastMessage;

    ui->groupTitleLabel->setText(groupName);
    ui->lastMessageLabel->setText(lastMessage.size() > 16 ? lastMessage.first(16) + "..." : lastMessage);

    quint64 secondsSinceMsg = timestamp/1000;
    QString format = calculateTimeFormat(secondsSinceMsg);

    ui->timestampLabel->setText(timestamp == 0 ? "" : QDateTime::fromSecsSinceEpoch(secondsSinceMsg).toString(format));
}

GroupWidget::~GroupWidget()
{
    delete ui;
}

QString GroupWidget::getId()
{
    return this->id;
}

qint64 GroupWidget::getTimestmap()
{
    return this->timestamp;
}

bool GroupWidget::getOpenedStatus()
{
    return this->wasOpened;
}

QString GroupWidget::getGroupName()
{
    return this->groupName;
}

QString GroupWidget::getOwnerId()
{
    return this->ownerId;
}

QString GroupWidget::getLastMessage()
{
    return this->lastMessage;
}

void GroupWidget::setOpenedStatus(bool status)
{
    this->wasOpened = status;
}

void GroupWidget::setLastMessage(QString lastMessage)
{
    ui->lastMessageLabel->setText(lastMessage);
    this->lastMessage = lastMessage;
}

void GroupWidget::setTimestamp(qint64 timestamp)
{
    quint64 secondsSinceMsg = timestamp/1000;
    QString format = calculateTimeFormat(secondsSinceMsg);

    ui->timestampLabel->setText(timestamp == 0 ? "" : QDateTime::fromSecsSinceEpoch(secondsSinceMsg).toString(format));
    this->timestamp = timestamp;
}

QString GroupWidget::calculateTimeFormat(quint64 secondsSinceMsg)
{
    quint64 secondsSinceEpoch = QDateTime::currentSecsSinceEpoch();
    QString format = "yyyy-MM-dd hh:mm";

    if (secondsSinceEpoch - secondsSinceMsg < 3600 * 24)
        format = "hh:mm";
    else if (secondsSinceEpoch - secondsSinceMsg < 3600 * 24 * 7)
        format = "ddd hh:mm";
    else if (secondsSinceEpoch - secondsSinceMsg < 3600 * 24 * 365)
        format = "d MMMM";

    return format;
}
