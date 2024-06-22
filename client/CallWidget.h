#ifndef CALLWIDGET_H
#define CALLWIDGET_H

#include <QJsonDocument>
#include <QWidget>

namespace Ui {
class CallWidget;
}

class CallWidget : public QWidget
{
    Q_OBJECT

public:
    explicit CallWidget(QWidget *parent = nullptr,
                        QString id = "",
                        QString groupId = "",
                        QString leaderId = "",
                        QString status = "",
                        QString title = "",
                        QString type = "",
                        qint64 scheduledTime = 0);
    ~CallWidget();

    QString getId();

Q_SIGNALS:
    void sendEvent(QJsonDocument eventData);

private slots:
    void on_joinButton_clicked();

private:
    Ui::CallWidget *ui;
    QString id;
    QString groupId;
    QString leaderId;
    QString status;
    QString title;
    QString type;
    qint64 scheduledTime;
    QTimer *timer;

    QPixmap *calendar_icon = new QPixmap(":/icons/calendar.png");
    QPixmap *phone_icon = new QPixmap(":/icons/phone.png");
};

#endif // CALLWIDGET_H
