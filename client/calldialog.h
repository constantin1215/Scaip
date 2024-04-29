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
    explicit CallDialog(QWidget *parent = nullptr);
    ~CallDialog();

private:
    Ui::CallDialog *ui;
};

#endif // CALLDIALOG_H
