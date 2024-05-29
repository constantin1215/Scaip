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

    ui->groupTitleLabel->setText(groupName);
    ui->lastMessageLabel->setText(lastMessage);
    ui->timestampLabel->setText(timestamp == 0 ? "" : QDateTime::fromSecsSinceEpoch(timestamp/1000).toString("yyyy-MM-dd hh:mm:ss"));
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

void GroupWidget::setOpenedStatus(bool status)
{
    this->wasOpened = status;
}

void GroupWidget::setLastMessage(QString lastMessage)
{
    ui->lastMessageLabel->setText(lastMessage);
}

void GroupWidget::setTimestamp(qint64 timestamp)
{
    ui->timestampLabel->setText(timestamp == 0 ? "" : QDateTime::fromSecsSinceEpoch(timestamp/1000).toString("yyyy-MM-dd hh:mm:ss"));
    this->timestamp = timestamp;
}
