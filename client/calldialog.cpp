#include "calldialog.h"
#include "ui_calldialog.h"

#include <QAudioOutput>
#include <QMediaPlayer>

CallDialog::CallDialog(QWidget *parent, QString groupTitle)
    : QDialog(parent)
    , ui(new Ui::CallDialog)
{
    ui->setupUi(this);

    ui->labelIcon->setPixmap(this->phone_icon->copy().scaled(128, 128, Qt::KeepAspectRatio));
    ui->buttonBox->setCursor(Qt::PointingHandCursor);

    ui->labelCallInfo->setText("Call in " + groupTitle);
}

CallDialog::~CallDialog()
{
    delete ui;
}
