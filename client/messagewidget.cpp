#include "messagewidget.h"
#include "ui_messagewidget.h"

#include <qdatetime.h>

MessageWidget::MessageWidget(QWidget *parent, QString id, QString userId, QString content, qint64 timestamp, QString username)
    : QWidget(parent)
    , ui(new Ui::MessageWidget)
{
    ui->setupUi(this);

    this->id = id;

    if (content.length() > 100 && !content.contains(' '))
        for (int i = 0; i < content.length() / 100; i++)
            content.insert((i + 1) * 100, '\n');

    ui->contentLabel->setText(content);
    ui->usernameLabel->setText(username);

    quint64 secondsSinceMsg = timestamp/1000;
    QString format = calculateTimeFormat(secondsSinceMsg);

    ui->timestampLabel->setText(timestamp == 0 ? "" : QDateTime::fromSecsSinceEpoch(secondsSinceMsg).toString(format));
}

MessageWidget::~MessageWidget()
{
    delete ui;
}

QString MessageWidget::calculateTimeFormat(quint64 secondsSinceMsg)
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
