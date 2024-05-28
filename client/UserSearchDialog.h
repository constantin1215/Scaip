#ifndef USERSEARCHDIALOG_H
#define USERSEARCHDIALOG_H

#include "qjsonarray.h"
#include <QDialog>
#include <QJsonDocument>
#include <QJsonObject>

namespace Ui {
class UserSearchDialog;
}

class UserSearchDialog : public QDialog
{
    Q_OBJECT

public:
    explicit UserSearchDialog(QWidget *parent = nullptr);
    ~UserSearchDialog();

    QJsonArray getChosenUsers();

public Q_SLOTS:
    void updateResultsList(QJsonObject eventData);
    void addToList(QString id);
    void removeFromList(QString id);

private slots:
    void on_buttonSearch_clicked();
Q_SIGNALS:
    void sendEvent(QJsonDocument eventData);
private:
    Ui::UserSearchDialog *ui;
    QJsonArray searchResults;
    QJsonArray chosenUsers;
};

#endif // USERSEARCHDIALOG_H
