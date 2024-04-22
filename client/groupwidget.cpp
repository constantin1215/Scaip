#include "groupwidget.h"
#include "ui_groupwidget.h"

#include <QDateTime>



GroupWidget::GroupWidget(QWidget *parent, QString id, QString groupName, QString lastMessage, qint64 timestamp)
    : QWidget(parent)
    , ui(new Ui::GroupWidget)
{
    ui->setupUi(this);

    this->id = id;
    this->groupName = groupName;
    this->timestamp = timestamp;
    this->wasOpened = false;

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

void GroupWidget::setOpenedStatus(bool status)
{
    this->wasOpened = status;
}
