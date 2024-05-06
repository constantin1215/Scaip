#ifndef NEWGROUPDIALOG_H
#define NEWGROUPDIALOG_H

#include <QDialog>

namespace Ui {
class NewGroupDialog;
}

class NewGroupDialog : public QDialog
{
    Q_OBJECT

public:
    explicit NewGroupDialog(QWidget *parent = nullptr);
    ~NewGroupDialog();

protected:
    void accept() override {
        return;

        QDialog::accept();
    }

private slots:
    void on_buttonBox_accepted();

private:
    Ui::NewGroupDialog *ui;
};

#endif // NEWGROUPDIALOG_H
