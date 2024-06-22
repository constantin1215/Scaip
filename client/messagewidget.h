#ifndef MESSAGEWIDGET_H
#define MESSAGEWIDGET_H

#include <QWidget>

enum class MessagesColor {
    OWNER,
    USER,
    OTHER
};

namespace Ui {
class MessageWidget;
}

class MessageWidget : public QWidget
{
    Q_OBJECT

public:
    explicit MessageWidget(QWidget *parent = nullptr, QString id = "", QString userId = "", QString content = "", qint64 timestamp = 0, QString username = "");
    ~MessageWidget();
    QString id;

    void changeUsernameColor(MessagesColor color);
    void changeTextBoxColor(MessagesColor color);
private:
    Ui::MessageWidget *ui;

    QString calculateTimeFormat(quint64 secondsSinceMsg);
};

#endif // MESSAGEWIDGET_H
