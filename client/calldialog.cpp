#include "calldialog.h"
#include "ui_calldialog.h"

CallDialog::CallDialog(QWidget *parent)
    : QDialog(parent)
    , ui(new Ui::CallDialog)
{
    ui->setupUi(this);
}

CallDialog::~CallDialog()
{
    delete ui;
}
