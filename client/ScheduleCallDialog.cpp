#include "ScheduleCallDialog.h"
#include "ui_ScheduleCallDialog.h"

ScheduleCallDialog::ScheduleCallDialog(QWidget *parent, QString groupTitle, QString groupId)
    : QDialog(parent)
    , ui(new Ui::ScheduleCallDialog)
{
    ui->setupUi(this);

    this->groupId = groupId;

    ui->labelMessage->setStyleSheet("color: #AA1111");
    ui->labelTitle->setText("Schedule call for " + groupTitle);
    ui->dateTimeEdit->setDateTime(QDateTime::currentDateTime());

    ui->buttonBox->setCursor(Qt::PointingHandCursor);
}

ScheduleCallDialog::~ScheduleCallDialog()
{
    delete ui;
}

QJsonDocument ScheduleCallDialog::getEvent()
{
    return this->event;
}

void ScheduleCallDialog::on_buttonBox_accepted()
{
    accept();
}

