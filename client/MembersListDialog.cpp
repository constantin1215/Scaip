#include "MembersListDialog.h"
#include "qjsondocument.h"
#include "qjsonobject.h"
#include "ui_MembersListDialog.h"

#include <UserData.h>
#include <UserWidget.h>
#include <mainwindow.h>

MembersListDialog::MembersListDialog(QWidget *parent, QString groupName, QString groupId, QString type)
    : QDialog(parent)
    , ui(new Ui::MembersListDialog)
{
    ui->setupUi(this);

    this->type = type;

    qDebug() << this->type;

    QObject::connect(this, &MembersListDialog::sendEvent, qobject_cast<MainWindow*>(parent), &MainWindow::sendEvent);

    ui->labelTitle->setText(groupName + "'s members");

    QJsonObject event;
    event.insert("EVENT", "FETCH_GROUP_MEMBERS");
    event.insert("groupId", groupId);
    event.insert("JWT", UserData::getInstance()->getJWT());

    QJsonDocument json(event);

    emit sendEvent(json);
}

MembersListDialog::~MembersListDialog()
{
    delete ui;
}

void MembersListDialog::updateMembersList(QJsonObject eventData)
{
    qDebug() << "Loading members list!";

    QJsonArray members = eventData["members"].toArray();

    for(int i = 0; i < members.count(); i++) {
        QJsonObject member = members.at(i).toObject();

        UserWidget *widget = new UserWidget(this,
                                            member["id"].toString(),
                                            member["username"].toString(),
                                            member["firstName"].toString(),
                                            member["lastName"].toString(),
                                            this->type == "OWNER" ? UserWidgetType::KICK : UserWidgetType::SIMPLE
                                            );

        QListWidgetItem *item = new QListWidgetItem(ui->listMembers);

        item->setSizeHint(widget->sizeHint());
        ui->listMembers->setItemWidget(item, widget);
    }
}
