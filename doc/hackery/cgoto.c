int main()
{
  goto *(&&l1);
  return 1;
l1:
  return 0;
l2:
  return 2;
}
