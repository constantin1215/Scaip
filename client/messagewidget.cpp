#include "messagewidget.h"
#include "ui_messagewidget.h"

#include <qdatetime.h>

MessageWidget::MessageWidget(QWidget *parent, QString id, QString userId, QString content, qint64 timestamp)
    : QWidget(parent)
    , ui(new Ui::MessageWidget)
{
    ui->setupUi(this);

    this->id = id;

    ui->contentLabel->setText(content);
    ui->usernameLabel->setText(userId);
    ui->timestampLabel->setText(timestamp == 0 ? "" : QDateTime::fromSecsSinceEpoch(timestamp/1000).toString("yyyy-MM-dd hh:mm:ss"));
}

MessageWidget::~MessageWidget()
{
    delete ui;
}
