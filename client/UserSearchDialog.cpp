#include "UserSearchDialog.h"
#include "qjsondocument.h"
#include "qjsonobject.h"
#include "ui_UserSearchDialog.h"

#include <UserData.h>
#include <UserWidget.h>

UserSearchDialog::UserSearchDialog(QWidget *parent)
    : QDialog(parent)
    , ui(new Ui::UserSearchDialog)
{
    ui->setupUi(this);
}

UserSearchDialog::~UserSearchDialog()
{
    QObject::disconnect(this);
    delete ui;
}

QJsonArray UserSearchDialog::getChosenUsers()
{
    return chosenUsers;
}

void UserSearchDialog::updateResultsList(QJsonObject eventData)
{
    qDebug() << "Updating results list";

    ui->listSearchResult->clear();

    QJsonArray members = eventData["members"].toArray();

    this->searchResults = members;

    for(int i = 0; i < members.count(); i++) {
        QJsonObject member = members.at(i).toObject();

        UserWidget *widget = new UserWidget(this,
                                            member["id"].toString(),
                                            member["username"].toString(),
                                            member["firstName"].toString(),
                                            member["lastName"].toString(),
                                            UserWidgetType::CHECKBOX
                                        );

        QListWidgetItem *item = new QListWidgetItem(ui->listSearchResult);

        item->setSizeHint(widget->sizeHint());
        ui->listSearchResult->setItemWidget(item, widget);

        QObject::connect(widget, &UserWidget::addToList, this, &UserSearchDialog::addToList);
        QObject::connect(widget, &UserWidget::removeFromList, this, &UserSearchDialog::removeFromList);
    }
}

void UserSearchDialog::addToList(QString id)
{
    for(int i = 0; i < this->searchResults.count(); i++) {
        QJsonObject member = this->searchResults.at(i).toObject();

        if (member["id"].toString() == id) {
            UserWidget *widget = new UserWidget(this,
                                                member["id"].toString(),
                                                member["username"].toString(),
                                                member["firstName"].toString(),
                                                member["lastName"].toString(),
                                                UserWidgetType::X_BTN
                                                );

            QListWidgetItem *item = new QListWidgetItem(ui->listChosenUsers);
            chosenUsers.push_back(member);

            item->setSizeHint(widget->sizeHint());
            ui->listChosenUsers->setItemWidget(item, widget);

            QObject::connect(widget, &UserWidget::addToList, this, &UserSearchDialog::addToList);
            QObject::connect(widget, &UserWidget::removeFromList, this, &UserSearchDialog::removeFromList);

            break;
        }
    }
}

void UserSearchDialog::removeFromList(QString id)
{
    for(int i = 0; i < ui->listChosenUsers->count(); i++) {
        QListWidgetItem* item = ui->listChosenUsers->item(i);

        UserWidget *widget = dynamic_cast<UserWidget *>(ui->listChosenUsers->itemWidget(item));

        if (widget->getId() == id) {
            ui->listChosenUsers->takeItem(i);

            for (int j = 0; j < chosenUsers.count(); ++j) {
                if (chosenUsers.at(j)["id"].toString() == id) {
                    chosenUsers.removeAt(j);
                    break;
                }
            }
            break;
        }
    }

    for(int i = 0; i < ui->listSearchResult->count(); i++) {
        QListWidgetItem* item = ui->listSearchResult->item(i);

        UserWidget *widget = dynamic_cast<UserWidget *>(ui->listSearchResult->itemWidget(item));

        if (widget->getId() == id) {
            widget->uncheckCheckbox();
            break;
        }
    }
}

void UserSearchDialog::on_buttonSearch_clicked()
{
    if (!ui->input_search_query->text().isEmpty() && !ui->input_search_query->text().isNull()) {
        QJsonObject event;
        event.insert("EVENT", "FETCH_USERS_BY_QUERY");
        event.insert("query", ui->input_search_query->text());
        event.insert("JWT", UserData::getInstance()->getJWT());

        QJsonDocument json(event);

        ui->listSearchResult->clear();

        emit sendEvent(json);
    }
}

