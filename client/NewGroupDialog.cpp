#include "NewGroupDialog.h"
#include "ui_NewGroupDialog.h"

NewGroupDialog::NewGroupDialog(QWidget *parent)
    : QDialog(parent)
    , ui(new Ui::NewGroupDialog)
{
    ui->setupUi(this);
}

NewGroupDialog::~NewGroupDialog()
{
    delete ui;
}

void NewGroupDialog::on_buttonBox_accepted()
{
    qDebug() << "hello";
    accept();
}

