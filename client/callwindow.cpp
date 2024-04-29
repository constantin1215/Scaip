#include "callwindow.h"
#include "ui_callwindow.h"

CallWindow::CallWindow(QWidget *parent, QJsonObject* eventData)
    : QDialog(parent)
    , ui(new Ui::CallWindow)
{
    ui->setupUi(this);
}

CallWindow::~CallWindow()
{
    delete ui;
}
