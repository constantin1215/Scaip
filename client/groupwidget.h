#ifndef GROUPWIDGET_H
#define GROUPWIDGET_H

#include <QEvent>
#include <QWidget>

namespace Ui {
class GroupWidget;
}

class GroupWidget : public QWidget
{
    Q_OBJECT

public:
    explicit GroupWidget(QWidget *parent = nullptr,
                         QString id = "",
                         QString groupName = "",
                         QString lastMessage = "",
                         qint64 timestamp = 0,
                         QString ownerId = "");
    ~GroupWidget();

    QString getId();
    qint64 getTimestmap();
    bool getOpenedStatus();
    QString getGroupName();
    QString getOwnerId();
    QString getLastMessage();

    void setOpenedStatus(bool status);
    void setLastMessage(QString lastMessage);
    void setTimestamp(qint64 timestamp);
private:
    Ui::GroupWidget *ui;
    QString id;
    QString ownerId;
    QString groupName;
    QString lastMessage;
    qint64 timestamp;
    bool wasOpened;

    QString calculateTimeFormat(quint64 secondsSinceMsg);
};

#endif // GROUPWIDGET_H
