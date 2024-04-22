#ifndef MESSAGEWIDGET_H
#define MESSAGEWIDGET_H

#include <QWidget>

namespace Ui {
class MessageWidget;
}

class MessageWidget : public QWidget
{
    Q_OBJECT

public:
    explicit MessageWidget(QWidget *parent = nullptr, QString id = "", QString userId = "", QString content = "", qint64 timestamp = 0);
    ~MessageWidget();

private:
    Ui::MessageWidget *ui;

    QString id;
};

#endif // MESSAGEWIDGET_H
