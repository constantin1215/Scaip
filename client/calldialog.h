#ifndef CALLDIALOG_H
#define CALLDIALOG_H

#include <QDialog>

namespace Ui {
class CallDialog;
}

class CallDialog : public QDialog
{
    Q_OBJECT

public:
    explicit CallDialog(QWidget *parent = nullptr, QString groupTitle = "");
    ~CallDialog();

private:
    Ui::CallDialog *ui;
    QPixmap *phone_icon = new QPixmap(":/icons/phone.png");
};

#endif // CALLDIALOG_H
