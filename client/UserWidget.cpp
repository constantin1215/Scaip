#include "UserWidget.h"
#include "UserSearchDialog.h"
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

    switch(type) {

    case UserWidgetType::CHECKBOX:
        ui->xButton->hide();
        ui->kickButton->hide();
        break;
    case UserWidgetType::X_BTN:
        ui->checkBox->hide();
        ui->kickButton->hide();
        break;
    case UserWidgetType::KICK:
        ui->checkBox->hide();
        ui->xButton->hide();
        break;
    case UserWidgetType::SIMPLE:
        ui->checkBox->hide();
        ui->xButton->hide();
        ui->kickButton->hide();
        break;
    }

    //QObject::connect(this, &UserWidget::addToList, qobject_cast<UserSearchDialog *>(parent), &UserSearchDialog::addToList);
    //QObject::connect(this, &UserWidget::removeFromList, qobject_cast<UserSearchDialog *>(parent), &UserSearchDialog::removeFromList);
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

