#ifndef MEMBERSLISTDIALOG_H
#define MEMBERSLISTDIALOG_H

#include <QDialog>
#include <QJsonDocument>
#include <QJsonObject>

namespace Ui {
class MembersListDialog;
}

class MembersListDialog : public QDialog
{
    Q_OBJECT

public:
    explicit MembersListDialog(QWidget *parent = nullptr, QString groupName = "", QString groupId = "", QString type = "");
    ~MembersListDialog();
public Q_SLOTS:
    void updateMembersList(QJsonObject eventData);

private:
    Ui::MembersListDialog *ui;
    QString type;
Q_SIGNALS:
    void sendEvent(QJsonDocument eventData);
};

#endif // MEMBERSLISTDIALOG_H
