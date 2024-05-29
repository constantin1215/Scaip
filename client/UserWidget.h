#ifndef USERWIDGET_H
#define USERWIDGET_H

#include <QWidget>

namespace Ui {
class UserWidget;
}

enum class UserWidgetType {
    CHECKBOX,
    X_BTN,
    SIMPLE,
    KICK
};

class UserWidget : public QWidget
{
    Q_OBJECT

public:
    explicit UserWidget(QWidget *parent = nullptr,
                        QString id = "",
                        QString username = "",
                        QString firstName = "",
                        QString lastName = "",
                        UserWidgetType type = UserWidgetType::SIMPLE);
    ~UserWidget();

    QString getId();
    void uncheckCheckbox();

private slots:
    void on_checkBox_stateChanged(int arg1);

    void on_xButton_clicked();

    void on_kickButton_clicked();

Q_SIGNALS:
    void addToList(QString id);
    void removeFromList(QString id);

private:
    Ui::UserWidget *ui;
    QString id;
    QString username;
    QString firstName;
    QString lastName;
};

#endif // USERWIDGET_H
