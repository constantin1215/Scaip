#include "messagewidget.h"
#include "ui_messagewidget.h"

#include <UserData.h>
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

    if (userId == UserData::getInstance()->getId()) {
        changeUsernameColor(MessagesColor::USER);
        changeTextBoxColor(MessagesColor::USER);
    }
    else {
        changeUsernameColor(MessagesColor::OTHER);
        changeTextBoxColor(MessagesColor::OTHER);
    }
}

MessageWidget::~MessageWidget()
{
    delete ui;
}

void MessageWidget::changeUsernameColor(MessagesColor color)
{
    switch (color) {
    case MessagesColor::OWNER:
        ui->usernameLabel->setStyleSheet("color: #c61e33");
        break;
    case MessagesColor::USER:
        ui->usernameLabel->setStyleSheet("color: #32CD32");
        break;
    case MessagesColor::OTHER:
        ui->usernameLabel->setStyleSheet("color: #1B99D4");
        break;
    }
}

void MessageWidget::changeTextBoxColor(MessagesColor color)
{
    switch (color) {
        case MessagesColor::OWNER:
            ui->contentLabel->setStyleSheet("QLabel { background-color: white; border: 2px solid #c61e33; padding: 5px; border-radius: 10px; }");
            break;
        case MessagesColor::USER:
            ui->contentLabel->setStyleSheet("QLabel { background-color: white; border: 2px solid #32CD32; padding: 5px; border-radius: 10px; }");
            break;
        case MessagesColor::OTHER:
            ui->contentLabel->setStyleSheet("QLabel { background-color: white; border: 2px solid #1B99D4; padding: 5px; border-radius: 10px; }");
            break;
    }
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
