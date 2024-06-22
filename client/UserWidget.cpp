#include "UserWidget.h"
#include "ui_UserWidget.h"

UserWidget::UserWidget(QWidget *parent, QString id, QString username, QString firstName, QString lastName, UserWidgetType type)
    : QWidget(parent)
    , ui(new Ui::UserWidget)
{
    ui->setupUi(this);

    this->id = id;
    this->username = username;
    this->firstName = firstName;
    this->lastName = lastName;

    ui->labelUsername->setText(username);
    ui->labelFirstName->setText(firstName);
    ui->labelLastName->setText(lastName);

    if (type == UserWidgetType::SIMPLE) {
    }

    ui->checkBox->hide();
    ui->xButton->hide();
    ui->kickButton->hide();
    ui->labelOwner->hide();

    switch(type) {

    case UserWidgetType::CHECKBOX:
        ui->checkBox->show();
        break;
    case UserWidgetType::X_BTN:
        ui->xButton->show();
        break;
    case UserWidgetType::KICK:
        ui->kickButton->show();
        break;
    case UserWidgetType::SIMPLE:
        break;
    case UserWidgetType::OWNER:
        ui->labelOwner->show();
        break;
    }

    ui->kickButton->setCursor(Qt::PointingHandCursor);
    ui->xButton->setCursor(Qt::PointingHandCursor);
    ui->checkBox->setCursor(Qt::PointingHandCursor);
    ui->kickButton->setStyleSheet("QPushButton { background-color: white; border: 2px solid #c61e33; border-radius: 10px; } QPushButton:hover { color: white; background-color: #c61e33;  }");
    ui->xButton->setStyleSheet("QPushButton { background-color: white; border: 2px solid #c61e33; border-radius: 10px; } QPushButton:hover { color: white; background-color: #c61e33;  }");
}

UserWidget::~UserWidget()
{
    QObject::disconnect(this);
    delete ui;
}

QString UserWidget::getId()
{
    return this->id;
}

void UserWidget::uncheckCheckbox()
{
    ui->checkBox->setCheckState(Qt::Unchecked);
}

void UserWidget::on_checkBox_stateChanged(int arg1)
{
    if (arg1 == 2)
        emit addToList(this->id);
    else
        emit removeFromList(this->id);
}


void UserWidget::on_xButton_clicked()
{
    emit removeFromList(this->id);
}


void UserWidget::on_kickButton_clicked()
{
    emit kick(this->id);
}

