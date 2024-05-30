#include "NewGroupDialog.h"
#include "UserSearchDialog.h"
#include "ui_NewGroupDialog.h"

#include <UserData.h>
#include <UserWidget.h>
#include <mainwindow.h>

NewGroupDialog::NewGroupDialog(QWidget *parent)
    : QDialog(parent)
    , ui(new Ui::NewGroupDialog)
{
    ui->setupUi(this);
    ui->messageLabel->setStyleSheet("color: #AA1111");
}

NewGroupDialog::~NewGroupDialog()
{
    QObject::disconnect(this);
    delete ui;
}

QString NewGroupDialog::getTitle()
{
    return this->title;
}

QString NewGroupDialog::getDescription()
{
    return this->description;
}

QJsonDocument NewGroupDialog::getEvent()
{
    return this->event;
}

void NewGroupDialog::triggerPassToSearchDialog(QJsonObject eventData)
{
    emit passToSearchDialog(eventData);
}

void NewGroupDialog::on_buttonBox_accepted()
{
    accept();
}


void NewGroupDialog::on_addMemberButton_clicked()
{
    ui->listMembersToAdd->clear();

    UserSearchDialog *userSearchDialog = new UserSearchDialog(this);
    QObject::connect(this, &NewGroupDialog::passToSearchDialog, userSearchDialog, &UserSearchDialog::updateResultsList);
    QObject::connect(userSearchDialog, &UserSearchDialog::sendEvent, qobject_cast<MainWindow*>(this->parent()), &MainWindow::sendEvent);
    userSearchDialog->exec();

    if (userSearchDialog->result() == QDialog::Accepted) {
        qDebug() << "Adding users to list!";

        members = userSearchDialog->getChosenUsers();

        for (int i = 0; i < members.count(); ++i) {
            QJsonObject member = members.at(i).toObject();

            UserWidget *widget = new UserWidget(this,
                                                member["id"].toString(),
                                                member["username"].toString(),
                                                member["firstName"].toString(),
                                                member["lastName"].toString()
                                                );

            QListWidgetItem *item = new QListWidgetItem(ui->listMembersToAdd);

            item->setSizeHint(widget->sizeHint());
            ui->listMembersToAdd->setItemWidget(item, widget);
        }
    }
}

